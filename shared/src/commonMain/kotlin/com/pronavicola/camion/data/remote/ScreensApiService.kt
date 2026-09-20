package com.pronavicola.camion.data.remote

import com.pronavicola.camion.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class RequestPairingCodeResponse(
    val deviceId: String,
    val pairingCode: String,
    val expiresInSeconds: Int,
)

data class FurgonInfo(
    val id: String,
    val plate: String,
    val brand: String? = null,
    val model: String? = null,
)

data class MqttConfigInfo(
    val brokerUrl: String?,
    val topicRoot: String?,
)

data class CheckPairingStatusResponse(
    val status: String,
    val deviceId: String,
    val deviceToken: String?,
    val furgon: FurgonInfo?,
    val mqttConfig: MqttConfigInfo?,
)

data class ValidateScreenResponse(
    val isValid: Boolean,
    val status: String,
    val furgonPlate: String?,
)

data class AvailableFurgonesResponse(
    val assignedFurgon: FurgonInfo?,
    val availableFurgones: List<FurgonInfo>,
)

class ScreensApiService {
    private val tag = "ScreensApiService"

    suspend fun requestPairingCode(
        baseUrl: String,
        hardwareFingerprint: String,
        deviceBrand: String,
        deviceModel: String,
        osVersion: String,
        appVersion: String,
    ): Result<RequestPairingCodeResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/pairing/request-code"
            val body = JSONObject().apply {
                put("hardware_fingerprint", hardwareFingerprint)
                put("device_brand", deviceBrand)
                put("device_model", deviceModel)
                put("os_version", osVersion)
                put("app_version", appVersion)
            }

            val responseJson = executePost(endpoint, body.toString())
            val resp = RequestPairingCodeResponse(
                deviceId = responseJson.optString("device_id"),
                pairingCode = responseJson.optString("pairing_code"),
                expiresInSeconds = responseJson.optInt("expires_in_seconds", 900),
            )
            Result.success(resp)
        } catch (e: Exception) {
            Log.e(tag, "requestPairingCode error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkPairingStatus(
        baseUrl: String,
        pairingCode: String,
        deviceId: String,
    ): Result<CheckPairingStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/pairing/check-status"
            val body = JSONObject().apply {
                put("pairing_code", pairingCode)
                put("device_id", deviceId)
            }

            val responseJson = executePost(endpoint, body.toString())
            val status = responseJson.optString("status", "PENDING_PAIRING")
            val devId = responseJson.optString("device_id", deviceId)
            val devToken = responseJson.optString("device_token", "").ifBlank { null }

            val furgonObj = responseJson.optJSONObject("furgon")
            val furgon = furgonObj?.let {
                FurgonInfo(
                    id = it.optString("id"),
                    plate = it.optString("plate"),
                    brand = if (it.has("brand")) it.getString("brand") else null,
                    model = if (it.has("model")) it.getString("model") else null,
                )
            }

            val mqttObj = responseJson.optJSONObject("mqtt_config")
            val mqttConfig = mqttObj?.let {
                MqttConfigInfo(
                    brokerUrl = it.optString("broker_url", "").takeIf { s -> s.isNotBlank() },
                    topicRoot = it.optString("topic_root", "").takeIf { s -> s.isNotBlank() },
                )
            }

            val resp = CheckPairingStatusResponse(
                status = status,
                deviceId = devId,
                deviceToken = devToken,
                furgon = furgon,
                mqttConfig = mqttConfig,
            )
            Result.success(resp)
        } catch (e: Exception) {
            Log.e(tag, "checkPairingStatus error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun activateLicenseKey(
        baseUrl: String,
        licenseKey: String,
        hardwareFingerprint: String,
        deviceBrand: String,
        deviceModel: String,
        osVersion: String,
        appVersion: String,
    ): Result<CheckPairingStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/pairing/activate-key"
            val body = JSONObject().apply {
                put("license_key", licenseKey)
                put("hardware_fingerprint", hardwareFingerprint)
                put("device_brand", deviceBrand)
                put("device_model", deviceModel)
                put("os_version", osVersion)
                put("app_version", appVersion)
            }

            val responseJson = executePost(endpoint, body.toString())
            val status = responseJson.optString("status", "ACTIVE")
            val devId = responseJson.optString("device_id")
            val devToken = responseJson.optString("device_token", "").ifBlank { null }

            val furgonObj = responseJson.optJSONObject("furgon")
            val furgon = furgonObj?.let {
                FurgonInfo(
                    id = it.optString("id"),
                    plate = it.optString("plate"),
                )
            }

            val mqttObj = responseJson.optJSONObject("mqtt_config")
            val mqttConfig = mqttObj?.let {
                MqttConfigInfo(
                    brokerUrl = it.optString("broker_url", "").takeIf { s -> s.isNotBlank() },
                    topicRoot = it.optString("topic_root", "").takeIf { s -> s.isNotBlank() },
                )
            }

            Result.success(
                CheckPairingStatusResponse(
                    status = status,
                    deviceId = devId,
                    deviceToken = devToken,
                    furgon = furgon,
                    mqttConfig = mqttConfig,
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "activateLicenseKey error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun validateScreen(
        baseUrl: String,
        deviceToken: String?,
        deviceId: String?,
    ): Result<ValidateScreenResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/validate"
            val responseJson = executeGet(endpoint, deviceToken, deviceId)
            val resp = ValidateScreenResponse(
                isValid = responseJson.optBoolean("is_valid", false),
                status = responseJson.optString("status", "UNKNOWN"),
                furgonPlate = responseJson.optString("furgon_plate", "").takeIf { it.isNotBlank() },
            )
            Result.success(resp)
        } catch (e: Exception) {
            Log.e(tag, "validateScreen error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAvailableFurgones(
        baseUrl: String,
        deviceToken: String?,
    ): Result<AvailableFurgonesResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/furgones"
            val responseJson = executeGet(endpoint, deviceToken, null)

            val assignedObj = responseJson.optJSONObject("assigned_furgon")
            val assigned = assignedObj?.let {
                FurgonInfo(
                    id = it.optString("id"),
                    plate = it.optString("plate"),
                    brand = it.optString("brand", "").takeIf { s -> s.isNotBlank() },
                )
            }

            val list = mutableListOf<FurgonInfo>()
            val arr = responseJson.optJSONArray("available_furgones") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(
                    FurgonInfo(
                        id = item.optString("id"),
                        plate = item.optString("plate"),
                        brand = item.optString("brand", "").takeIf { s -> s.isNotBlank() },
                    )
                )
            }

            Result.success(AvailableFurgonesResponse(assigned, list))
        } catch (e: Exception) {
            Log.e(tag, "getAvailableFurgones error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateAssociatedFurgon(
        baseUrl: String,
        deviceToken: String?,
        furgonId: String,
    ): Result<FurgonInfo> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/furgon"
            val body = JSONObject().apply {
                put("furgon_id", furgonId)
            }

            val responseJson = executePatch(endpoint, body.toString(), deviceToken)
            val furgonObj = responseJson.optJSONObject("furgon") ?: responseJson
            val furgon = FurgonInfo(
                id = furgonObj.optString("id"),
                plate = furgonObj.optString("plate"),
            )
            Result.success(furgon)
        } catch (e: Exception) {
            Log.e(tag, "updateAssociatedFurgon error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendPing(
        baseUrl: String,
        deviceToken: String?,
        batteryLevel: Int,
        isCharging: Boolean,
        networkType: String,
        appVersion: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/ping"
            val body = JSONObject().apply {
                put("battery_level", batteryLevel)
                put("is_charging", isCharging)
                put("network_type", networkType)
                put("app_version", appVersion)
            }

            executePost(endpoint, body.toString(), deviceToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "sendPing error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun unpairDevice(
        baseUrl: String,
        deviceToken: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${cleanBaseUrl(baseUrl)}/api/v1/screens/unpair"
            val body = JSONObject()
            executePost(endpoint, body.toString(), deviceToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "unpairDevice error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun executePost(endpoint: String, jsonBody: String, authBearer: String? = null): JSONObject {
        return executeHttp("POST", endpoint, jsonBody, authBearer)
    }

    private fun executePatch(endpoint: String, jsonBody: String, authBearer: String? = null): JSONObject {
        return executeHttp("PATCH", endpoint, jsonBody, authBearer)
    }

    private fun executeGet(endpoint: String, authBearer: String? = null, deviceIdHeader: String? = null): JSONObject {
        return executeHttp("GET", endpoint, null, authBearer, deviceIdHeader)
    }

    class ApiException(val code: Int, message: String) : Exception(message)

    private fun executeHttp(
        method: String,
        endpoint: String,
        jsonBody: String?,
        authBearer: String? = null,
        deviceIdHeader: String? = null,
    ): JSONObject {
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")

        if (!authBearer.isNullAndBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $authBearer")
        }
        if (!deviceIdHeader.isNullAndBlank()) {
            conn.setRequestProperty("X-Device-Id", deviceIdHeader)
        }

        if (jsonBody != null && (method == "POST" || method == "PATCH" || method == "PUT")) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(jsonBody)
                it.flush()
            }
        }

        val responseCode = conn.responseCode
        val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val responseText = reader.use { it.readText() }

        if (responseCode !in 200..299) {
            val errMsg = try {
                JSONObject(responseText).optString("message", "HTTP $responseCode")
            } catch (_: Exception) {
                "HTTP $responseCode: $responseText"
            }
            throw ApiException(responseCode, errMsg)
        }

        return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
    }

    private fun cleanBaseUrl(url: String): String {
        return url.trim().removeSuffix("/")
    }

    private fun String?.isNullAndBlank(): Boolean = this.isNullOrBlank()
}
