package com.iatb.materialthemes.data

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.iatb.materialthemes.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread
import kotlin.math.roundToInt

data class HourlyForecast(
    val timeLabel: String,
    val tempLabel: String,
    val iconResId: Int
)

data class WeatherData(
    val locationName: String,
    val currentTempLabel: String,
    val conditionLabel: String,
    val tempRangeLabel: String,
    val maxTempLabel: String = "31°",
    val minTempLabel: String = "23°",
    val humidityLabel: String,
    val windLabel: String,
    val currentIconResId: Int,
    val hourlyForecasts: List<HourlyForecast>
)

object WeatherRepository {

    private const val TAG = "WeatherRepository"
    private const val PREFS_NAME = "weather_cache_prefs"
    private const val KEY_LAST_FETCH = "key_last_fetch_time"
    private const val KEY_WEATHER_DATA = "key_cached_weather_json"
    private const val KEY_CACHED_LAT = "key_cached_lat"
    private const val KEY_CACHED_LON = "key_cached_lon"
    private const val KEY_CACHED_CITY = "key_cached_city"
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    data class LocationInfo(
        val lat: Double,
        val lon: Double,
        val cityName: String? = null
    )

    // Fallback coordinates (Hanoi, Vietnam)
    private const val DEFAULT_LAT = 21.0285
    private const val DEFAULT_LON = 105.8542
    private const val DEFAULT_CITY = "Hanoi"

    @Volatile
    private var cachedInMemory: WeatherData? = null

    @Volatile
    private var isFetching = false

    fun getWeatherData(context: Context): WeatherData {
        val inMem = cachedInMemory
        if (inMem != null) {
            checkAndScheduleRefreshIfStale(context)
            return inMem
        }

        // Try reading from SharedPreferences cache
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_WEATHER_DATA, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val data = parseStoredJson(context, JSONObject(jsonStr))
                cachedInMemory = data
                checkAndScheduleRefreshIfStale(context)
                return data
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing cached weather", e)
            }
        }

        // Trigger immediate fetch in background
        refreshWeather(context, null)
        return getDefaultWeatherData(context)
    }

    fun refreshWeather(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        if (isFetching) {
            Log.d(TAG, "Weather fetch already in progress, skipping duplicate request")
            onComplete?.invoke(false)
            return
        }

        val appContext = context.applicationContext
        thread(name = "WeatherFetchThread") {
            isFetching = true
            try {
                Log.d(TAG, "Starting performFetch...")
                val success = performFetch(appContext)
                Log.d(TAG, "performFetch completed with result: $success")
                if (success) {
                    val updateIntent = Intent("com.iatb.materialthemes.ACTION_WIDGET_TICK").apply {
                        setPackage(appContext.packageName)
                    }
                    appContext.sendBroadcast(updateIntent)
                }
                onComplete?.invoke(success)
            } finally {
                isFetching = false
            }
        }
    }

    private fun checkAndScheduleRefreshIfStale(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getLong(KEY_LAST_FETCH, 0L)
        val now = System.currentTimeMillis()
        if (now - lastFetch > CACHE_DURATION_MS) {
            refreshWeather(context, null)
        }
    }

    private fun performFetch(context: Context): Boolean {
        val locInfo = getBestCoordinates(context)
        val lat = locInfo.lat
        val lon = locInfo.lon
        Log.d(TAG, "Using coordinates: lat=$lat, lon=$lon")

        val cityName = resolveLocationName(context, lat, lon, locInfo.cityName)
        Log.d(TAG, "Resolved city name: $cityName")

        // 1. Try Primary Weather Provider: Open-Meteo
        var weatherData = fetchFromOpenMeteo(context, lat, lon, cityName)

        // 2. If Primary Provider fails, try Fallback Provider: wttr.in
        if (weatherData == null) {
            Log.w(TAG, "Open-Meteo failed, trying secondary provider: wttr.in")
            weatherData = fetchFromWttrIn(context, lat, lon, cityName)
        }

        if (weatherData != null) {
            // Cache to SharedPreferences
            val serializedJson = serializeWeatherData(weatherData)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                .putString(KEY_WEATHER_DATA, serializedJson.toString())
                .apply()

            cachedInMemory = weatherData
            Log.i(TAG, "Weather successfully updated: ${weatherData.locationName}, ${weatherData.currentTempLabel}, ${weatherData.conditionLabel}")
            return true
        }

        Log.e(TAG, "All weather providers failed to fetch data")
        return false
    }

    private fun fetchFromOpenMeteo(
        context: Context,
        lat: Double,
        lon: Double,
        cityName: String
    ): WeatherData? {
        return try {
            val apiUrl = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                    "&hourly=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto"

            Log.d(TAG, "Connecting to Open-Meteo: $apiUrl")
            val conn = openResilientHttpsConnection(apiUrl)

            val responseCode = conn.responseCode
            Log.d(TAG, "Open-Meteo HTTP response code: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            conn.disconnect()

            val rawJson = JSONObject(sb.toString())
            parseOpenMeteoJson(context, rawJson, cityName)
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching from Open-Meteo: ${e.javaClass.name}: ${e.message}", e)
            null
        }
    }

    private fun fetchFromWttrIn(
        context: Context,
        lat: Double,
        lon: Double,
        fallbackCityName: String
    ): WeatherData? {
        return try {
            val apiUrl = "https://wttr.in/$lat,$lon?format=j1"
            Log.d(TAG, "Connecting to wttr.in: $apiUrl")
            val conn = openResilientHttpsConnection(apiUrl)

            val responseCode = conn.responseCode
            Log.d(TAG, "wttr.in HTTP response code: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            conn.disconnect()

            val rawJson = JSONObject(sb.toString())
            parseWttrInJson(context, rawJson, fallbackCityName)
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching from wttr.in: ${e.javaClass.name}: ${e.message}", e)
            null
        }
    }

    /**
     * Resilient HTTPS connection wrapper that handles DNS timeouts, Private DNS failures,
     * and ISP filtering via IP routing while maintaining SNI and SSL certificate verification.
     */
    private fun openResilientHttpsConnection(urlStr: String): HttpsURLConnection {
        val originalUrl = URL(urlStr)
        val host = originalUrl.host

        // If system DNS can resolve host, use originalUrl directly for optimal HTTP/2 & CDN support
        val systemCanResolve = try {
            InetAddress.getByName(host) != null
        } catch (e: Exception) {
            false
        }

        if (systemCanResolve) {
            Log.d(TAG, "System DNS active, connecting directly to $host")
            val conn = originalUrl.openConnection() as HttpsURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "MaterialThemesApp/1.0 (Android)")
            return conn
        }

        val resolvedIp = resolveHostToIp(host)
        val connectionUrl = if (resolvedIp != null) {
            URL("https://${resolvedIp.hostAddress}${originalUrl.file}")
        } else {
            originalUrl
        }

        Log.d(TAG, "System DNS failed, opening fallback connection to: $connectionUrl (target host: $host)")
        val conn = connectionUrl.openConnection() as HttpsURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Host", host)
        conn.setRequestProperty("User-Agent", "MaterialThemesApp/1.0 (Android)")

        if (resolvedIp != null) {
            val defaultSslFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
            conn.sslSocketFactory = object : SSLSocketFactory() {
                override fun getDefaultCipherSuites(): Array<String> = defaultSslFactory.defaultCipherSuites
                override fun getSupportedCipherSuites(): Array<String> = defaultSslFactory.supportedCipherSuites

                private fun configureSni(socket: Socket): Socket {
                    if (socket is javax.net.ssl.SSLSocket && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        try {
                            val params = socket.sslParameters
                            params.serverNames = listOf(javax.net.ssl.SNIHostName(host))
                            socket.sslParameters = params
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed setting SNI", e)
                        }
                    }
                    return socket
                }

                override fun createSocket(s: Socket, h: String, p: Int, autoClose: Boolean): Socket {
                    return configureSni(defaultSslFactory.createSocket(s, host, p, autoClose))
                }

                override fun createSocket(h: String, p: Int): Socket {
                    return configureSni(defaultSslFactory.createSocket(resolvedIp, p))
                }

                override fun createSocket(h: String, p: Int, localHost: InetAddress, localPort: Int): Socket {
                    return configureSni(defaultSslFactory.createSocket(resolvedIp, p, localHost, localPort))
                }

                override fun createSocket(h: InetAddress, p: Int): Socket {
                    return configureSni(defaultSslFactory.createSocket(resolvedIp, p))
                }

                override fun createSocket(address: InetAddress, p: Int, localAddress: InetAddress, localPort: Int): Socket {
                    return configureSni(defaultSslFactory.createSocket(resolvedIp, p, localAddress, localPort))
                }
            }

            conn.hostnameVerifier = HostnameVerifier { _, session ->
                HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session)
            }
        }

        return conn
    }

    private fun resolveHostToIp(host: String): InetAddress? {
        // 1. Try standard system DNS (short timeout)
        try {
            val addr = InetAddress.getByName(host)
            if (addr != null) {
                Log.d(TAG, "Resolved $host via system DNS: ${addr.hostAddress}")
                return addr
            }
        } catch (e: Exception) {
            Log.w(TAG, "System DNS failed for $host: ${e.message}")
        }

        // 2. Try DNS-over-HTTPS via 1.1.1.1 (Cloudflare DNS JSON API)
        try {
            val dohUrl = URL("https://1.1.1.1/dns-query?name=$host&type=A")
            val dohConn = (dohUrl.openConnection() as HttpsURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/dns-json")
                setRequestProperty("User-Agent", "MaterialThemes/1.0")
            }
            if (dohConn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = dohConn.inputStream.bufferedReader().readText()
                dohConn.disconnect()
                val json = JSONObject(jsonStr)
                val answers = json.optJSONArray("Answer")
                if (answers != null && answers.length() > 0) {
                    for (i in 0 until answers.length()) {
                        val ans = answers.getJSONObject(i)
                        val ipStr = ans.optString("data", "")
                        val parts = ipStr.split(".")
                        if (parts.size == 4) {
                            val bytes = ByteArray(4) { idx -> parts[idx].toInt().toByte() }
                            val resolved = InetAddress.getByAddress(host, bytes)
                            Log.i(TAG, "Successfully resolved $host via DoH 1.1.1.1 to $ipStr")
                            return resolved
                        }
                    }
                }
            } else {
                dohConn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH 1.1.1.1 failed for $host: ${e.message}")
        }

        // 3. Fallback to hardcoded known IPs
        val fallbackIp = when (host) {
            "api.open-meteo.com" -> "188.40.99.226"
            "nominatim.openstreetmap.org" -> "184.104.179.137"
            "wttr.in" -> "5.9.243.187"
            else -> null
        }

        if (fallbackIp != null) {
            val parts = fallbackIp.split(".")
            if (parts.size == 4) {
                val bytes = ByteArray(4) { idx -> parts[idx].toInt().toByte() }
                Log.i(TAG, "Using fallback hardcoded IP for $host: $fallbackIp")
                return InetAddress.getByAddress(host, bytes)
            }
        }

        return null
    }

    private fun getBestCoordinates(context: Context): LocationInfo {
        // 1. Try GPS / Network location if permission is granted
        try {
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locManager != null) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    val netLoc = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val gpsLoc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val best: Location? = when {
                        netLoc != null && gpsLoc != null -> if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                        gpsLoc != null -> gpsLoc
                        else -> netLoc
                    }
                    if (best != null && (System.currentTimeMillis() - best.time < 24 * 3600 * 1000L)) {
                        return LocationInfo(best.latitude, best.longitude, null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire device location", e)
        }

        // 2. IP-based Geolocation fallback (works without GPS permission)
        val ipLoc = fetchIpLocation()
        if (ipLoc != null) {
            saveCachedLocation(context, ipLoc)
            return ipLoc
        }

        // 3. Try reading previously cached location
        val cached = getCachedLocation(context)
        if (cached != null) {
            return cached
        }

        return LocationInfo(DEFAULT_LAT, DEFAULT_LON, DEFAULT_CITY)
    }

    private fun fetchIpLocation(): LocationInfo? {
        // Try https://ipwho.is/
        try {
            val conn = openResilientHttpsConnection("https://ipwho.is/")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(text)
                if (json.optBoolean("success", false)) {
                    val lat = json.getDouble("latitude")
                    val lon = json.getDouble("longitude")
                    val city = json.optString("city", "").ifEmpty { json.optString("region", "") }
                    Log.i(TAG, "Resolved IP geolocation from ipwho.is: city=$city, lat=$lat, lon=$lon")
                    return LocationInfo(lat, lon, city.ifEmpty { null })
                }
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed ipwho.is geolocation", e)
        }

        // Fallback: http://ip-api.com/json
        try {
            val url = URL("http://ip-api.com/json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(text)
                if (json.optString("status") == "success") {
                    val lat = json.getDouble("lat")
                    val lon = json.getDouble("lon")
                    val city = json.optString("city", "").ifEmpty { json.optString("regionName", "") }
                    Log.i(TAG, "Resolved IP geolocation from ip-api.com: city=$city, lat=$lat, lon=$lon")
                    return LocationInfo(lat, lon, city.ifEmpty { null })
                }
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed ip-api.com geolocation", e)
        }

        return null
    }

    private fun saveCachedLocation(context: Context, loc: LocationInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CACHED_LAT, loc.lat.toString())
            .putString(KEY_CACHED_LON, loc.lon.toString())
            .putString(KEY_CACHED_CITY, loc.cityName ?: "")
            .apply()
    }

    private fun getCachedLocation(context: Context): LocationInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latStr = prefs.getString(KEY_CACHED_LAT, null) ?: return null
        val lonStr = prefs.getString(KEY_CACHED_LON, null) ?: return null
        val city = prefs.getString(KEY_CACHED_CITY, null)?.ifEmpty { null }
        return try {
            LocationInfo(latStr.toDouble(), lonStr.toDouble(), city)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveLocationName(
        context: Context,
        lat: Double,
        lon: Double,
        suggestedCity: String?
    ): String {
        // 1. Try Android Geocoder first (prefer City/Province level adminArea over subAdminArea district)
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lon, 1)
            val address = list?.firstOrNull()
            if (address != null) {
                val city = address.adminArea
                    ?: address.locality
                    ?: address.subAdminArea
                if (!city.isNullOrEmpty()) return city
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder failed, trying Nominatim OpenStreetMap reverse", e)
        }

        // 2. OpenStreetMap Nominatim reverse geocode fallback using resilient connection
        // Prioritize city -> state (Province) -> town -> county (District)
        try {
            val nomUrl = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=10"
            val conn = openResilientHttpsConnection(nomUrl)
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val obj = JSONObject(text)
                val address = obj.optJSONObject("address")
                if (address != null) {
                    val city = address.optString("city", "")
                        .ifEmpty { address.optString("state", "") }
                        .ifEmpty { address.optString("town", "") }
                        .ifEmpty { address.optString("county", "") }
                    if (city.isNotEmpty()) return city
                }
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim reverse failed", e)
        }

        if (!suggestedCity.isNullOrEmpty()) {
            return suggestedCity
        }

        return DEFAULT_CITY
    }

    private fun parseOpenMeteoJson(context: Context, json: JSONObject, locationName: String): WeatherData {
        val currentObj = json.getJSONObject("current")
        val currentTemp = currentObj.getDouble("temperature_2m").roundToInt()
        val weatherCode = currentObj.getInt("weather_code")
        val humidity = currentObj.optInt("relative_humidity_2m", 65)
        val windSpeed = currentObj.optDouble("wind_speed_10m", 12.0).roundToInt()

        val dailyObj = json.optJSONObject("daily")
        val maxTemp = dailyObj?.optJSONArray("temperature_2m_max")?.optDouble(0, currentTemp + 4.0)?.roundToInt() ?: (currentTemp + 4)
        val minTemp = dailyObj?.optJSONArray("temperature_2m_min")?.optDouble(0, currentTemp - 4.0)?.roundToInt() ?: (currentTemp - 4)

        val conditionRes = mapWeatherCodeToStringRes(weatherCode)
        val conditionStr = context.getString(conditionRes)
        val iconRes = mapWeatherCodeToDrawable(weatherCode)

        // Parse hourly forecast: 6 slots starting from current hour
        val hourlyList = mutableListOf<HourlyForecast>()
        val hourlyObj = json.optJSONObject("hourly")
        if (hourlyObj != null) {
            val timeArray = hourlyObj.optJSONArray("time")
            val tempArray = hourlyObj.optJSONArray("temperature_2m")
            val codeArray = hourlyObj.optJSONArray("weather_code")

            if (timeArray != null && tempArray != null) {
                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.getDefault()).format(Date())
                var startIndex = 0
                for (i in 0 until timeArray.length()) {
                    if (timeArray.optString(i) >= nowStr) {
                        startIndex = i
                        break
                    }
                }

                var count = 0
                var idx = startIndex
                while (count < 6 && idx < timeArray.length()) {
                    val rawTime = timeArray.optString(idx)
                    val hourLabel = formatIsoTimeToHour(rawTime)
                    val tempVal = tempArray.optDouble(idx, currentTemp.toDouble()).roundToInt()
                    val hCode = codeArray?.optInt(idx, weatherCode) ?: weatherCode
                    val hIcon = mapWeatherCodeToDrawable(hCode)

                    hourlyList.add(HourlyForecast(hourLabel, "$tempVal°", hIcon))
                    count++
                    idx += 2 // Step 2 hours for broad forecast range
                }
            }
        }

        if (hourlyList.size < 6) {
            val defaults = getDefaultHourlyList(context)
            while (hourlyList.size < 6 && hourlyList.size < defaults.size) {
                hourlyList.add(defaults[hourlyList.size])
            }
        }

        return WeatherData(
            locationName = locationName,
            currentTempLabel = "$currentTemp°",
            conditionLabel = conditionStr,
            tempRangeLabel = "$maxTemp° / $minTemp°",
            maxTempLabel = "$maxTemp°",
            minTempLabel = "$minTemp°",
            humidityLabel = "$humidity%",
            windLabel = "$windSpeed km/h",
            currentIconResId = iconRes,
            hourlyForecasts = hourlyList
        )
    }

    private fun parseWttrInJson(context: Context, json: JSONObject, fallbackCityName: String): WeatherData {
        val currentArray = json.getJSONArray("current_condition")
        val currentObj = currentArray.getJSONObject(0)
        val tempC = currentObj.optString("temp_C", "28").toIntOrNull() ?: 28
        val humidity = currentObj.optString("humidity", "65")
        val windKmph = currentObj.optString("windspeedKmph", "12")
        val descArray = currentObj.optJSONArray("weatherDesc")
        val rawDesc = descArray?.optJSONObject(0)?.optString("value", "Partly cloudy") ?: "Partly cloudy"

        val weatherArray = json.optJSONArray("weather")
        val todayObj = weatherArray?.optJSONObject(0)
        val maxTemp = todayObj?.optString("maxtempC", "${tempC + 3}")?.toIntOrNull() ?: (tempC + 3)
        val minTemp = todayObj?.optString("mintempC", "${tempC - 3}")?.toIntOrNull() ?: (tempC - 3)

        val nearestArea = json.optJSONArray("nearest_area")?.optJSONObject(0)
        val areaName = nearestArea?.optJSONArray("areaName")?.optJSONObject(0)?.optString("value")
            ?: fallbackCityName

        val conditionRes = mapDescToStringRes(rawDesc)
        val conditionStr = context.getString(conditionRes)
        val iconRes = mapDescToDrawable(rawDesc)

        val hourlyList = mutableListOf<HourlyForecast>()
        val hourlyArr = todayObj?.optJSONArray("hourly")
        if (hourlyArr != null) {
            val nowHour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toIntOrNull() ?: 12
            for (i in 0 until hourlyArr.length()) {
                val hObj = hourlyArr.getJSONObject(i)
                val rawTime = hObj.optString("time", "0")
                val hourInt = (rawTime.toIntOrNull() ?: 0) / 100
                if (hourInt >= nowHour || hourlyList.isNotEmpty()) {
                    val hTemp = hObj.optString("tempC", "$tempC")
                    val hDesc = hObj.optJSONArray("weatherDesc")?.optJSONObject(0)?.optString("value", "Partly cloudy") ?: "Partly cloudy"
                    val hIcon = mapDescToDrawable(hDesc)
                    val label = String.format(Locale.getDefault(), "%02d:00", hourInt)
                    hourlyList.add(HourlyForecast(label, "$hTemp°", hIcon))
                    if (hourlyList.size >= 6) break
                }
            }
        }
        if (hourlyList.size < 6) {
            val defaults = getDefaultHourlyList(context)
            while (hourlyList.size < 6 && hourlyList.size < defaults.size) {
                hourlyList.add(defaults[hourlyList.size])
            }
        }

        return WeatherData(
            locationName = areaName,
            currentTempLabel = "$tempC°",
            conditionLabel = conditionStr,
            tempRangeLabel = "$maxTemp° / $minTemp°",
            maxTempLabel = "$maxTemp°",
            minTempLabel = "$minTemp°",
            humidityLabel = "$humidity%",
            windLabel = "$windKmph km/h",
            currentIconResId = iconRes,
            hourlyForecasts = hourlyList
        )
    }

    private fun mapDescToStringRes(desc: String): Int {
        val d = desc.lowercase(Locale.ROOT)
        return when {
            d.contains("sunny") || d.contains("clear") -> R.string.weather_clear
            d.contains("thunder") -> R.string.weather_thunder
            d.contains("snow") || d.contains("blizzard") -> R.string.weather_snow
            d.contains("rain") || d.contains("shower") -> R.string.weather_rainy
            d.contains("drizzle") -> R.string.weather_drizzle
            d.contains("fog") || d.contains("mist") -> R.string.weather_fog
            d.contains("overcast") -> R.string.weather_overcast
            d.contains("cloud") -> R.string.weather_partly_cloudy
            else -> R.string.weather_partly_cloudy
        }
    }

    private fun mapDescToDrawable(desc: String): Int {
        val d = desc.lowercase(Locale.ROOT)
        return when {
            d.contains("sunny") || d.contains("clear") -> R.drawable.ic_weather_sunny
            d.contains("rain") || d.contains("shower") || d.contains("drizzle") || d.contains("thunder") -> R.drawable.ic_weather_rainy
            else -> R.drawable.ic_weather_partly_cloudy
        }
    }

    private fun mapWeatherCodeToStringRes(code: Int): Int {
        return when (code) {
            0 -> R.string.weather_clear
            1, 2 -> R.string.weather_partly_cloudy
            3 -> R.string.weather_overcast
            45, 48 -> R.string.weather_fog
            51, 53, 55 -> R.string.weather_drizzle
            61, 63, 65, 80, 81, 82 -> R.string.weather_rainy
            71, 73, 75, 77, 85, 86 -> R.string.weather_snow
            95, 96, 99 -> R.string.weather_thunder
            else -> R.string.weather_partly_cloudy
        }
    }

    private fun mapWeatherCodeToDrawable(code: Int): Int {
        return when (code) {
            0 -> R.drawable.ic_weather_sunny
            1, 2 -> R.drawable.ic_weather_partly_cloudy
            3, 45, 48 -> R.drawable.ic_weather_night_cloudy
            51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99 -> R.drawable.ic_weather_rainy
            else -> R.drawable.ic_weather_partly_cloudy
        }
    }

    private fun formatIsoTimeToHour(isoTime: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val date = parser.parse(isoTime)
            if (date != null) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else {
                isoTime.substringAfter("T").take(5)
            }
        } catch (e: Exception) {
            isoTime.substringAfter("T").take(5)
        }
    }

    private fun serializeWeatherData(data: WeatherData): JSONObject {
        val json = JSONObject()
        json.put("location", data.locationName)
        json.put("temp", data.currentTempLabel)
        json.put("condition", data.conditionLabel)
        json.put("range", data.tempRangeLabel)
        json.put("maxTemp", data.maxTempLabel)
        json.put("minTemp", data.minTempLabel)
        json.put("humidity", data.humidityLabel)
        json.put("wind", data.windLabel)
        json.put("icon", data.currentIconResId)

        val array = JSONArray()
        data.hourlyForecasts.forEach { h ->
            val obj = JSONObject()
            obj.put("time", h.timeLabel)
            obj.put("temp", h.tempLabel)
            obj.put("icon", h.iconResId)
            array.put(obj)
        }
        json.put("hourly", array)
        return json
    }

    private fun parseStoredJson(context: Context, json: JSONObject): WeatherData {
        val loc = json.optString("location", DEFAULT_CITY)
        val temp = json.optString("temp", "28°")
        val cond = json.optString("condition", context.getString(R.string.weather_partly_cloudy))
        val range = json.optString("range", "↑ 31°  ↓ 23°")
        val maxT = json.optString("maxTemp", "31°")
        val minT = json.optString("minTemp", "23°")
        val hum = json.optString("humidity", "65%")
        val wind = json.optString("wind", "12 km/h")
        val icon = json.optInt("icon", R.drawable.ic_weather_night_cloudy)

        val hourlyList = mutableListOf<HourlyForecast>()
        val array = json.optJSONArray("hourly")
        if (array != null) {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                hourlyList.add(
                    HourlyForecast(
                        timeLabel = obj.getString("time"),
                        tempLabel = obj.getString("temp"),
                        iconResId = obj.getInt("icon")
                    )
                )
            }
        }

        return WeatherData(
            locationName = loc,
            currentTempLabel = temp,
            conditionLabel = cond,
            tempRangeLabel = range,
            maxTempLabel = maxT,
            minTempLabel = minT,
            humidityLabel = hum,
            windLabel = wind,
            currentIconResId = icon,
            hourlyForecasts = if (hourlyList.isNotEmpty()) hourlyList else getDefaultHourlyList(context)
        )
    }

    private fun getDefaultWeatherData(context: Context): WeatherData {
        return WeatherData(
            locationName = DEFAULT_CITY,
            currentTempLabel = context.getString(R.string.sample_temp_11),
            conditionLabel = context.getString(R.string.sample_weather_condition),
            tempRangeLabel = context.getString(R.string.sample_temp_range),
            maxTempLabel = "31°",
            minTempLabel = "23°",
            humidityLabel = context.getString(R.string.sample_humidity),
            windLabel = context.getString(R.string.sample_wind),
            currentIconResId = R.drawable.ic_weather_night_cloudy,
            hourlyForecasts = getDefaultHourlyList(context)
        )
    }

    private fun getDefaultHourlyList(context: Context): List<HourlyForecast> {
        return listOf(
            HourlyForecast(context.getString(R.string.sample_forecast_time_1), context.getString(R.string.sample_forecast_temp_1), R.drawable.ic_weather_sunny),
            HourlyForecast(context.getString(R.string.sample_forecast_time_2), context.getString(R.string.sample_forecast_temp_2), R.drawable.ic_weather_sunny),
            HourlyForecast(context.getString(R.string.sample_forecast_time_3), context.getString(R.string.sample_forecast_temp_3), R.drawable.ic_weather_partly_cloudy),
            HourlyForecast(context.getString(R.string.sample_forecast_time_4), context.getString(R.string.sample_forecast_temp_4), R.drawable.ic_weather_rainy),
            HourlyForecast(context.getString(R.string.sample_forecast_time_5), context.getString(R.string.sample_forecast_temp_5), R.drawable.ic_weather_partly_cloudy),
            HourlyForecast(context.getString(R.string.sample_forecast_time_6), context.getString(R.string.sample_forecast_temp_6), R.drawable.ic_weather_partly_cloudy)
        )
    }
}
