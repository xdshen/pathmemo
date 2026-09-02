package com.pathmemo.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager

/**
 * Snapshot of the serving cell (4G/5G base station) at a moment in time.
 * Fields may be null when the modem does not report them.
 */
data class CellInfoSnapshot(
    val networkType: String,
    val operatorName: String?,
    val mcc: String?,
    val mnc: String?,
    val tac: Int?,
    val pci: Int?,
    val ci: Long?,
    val arfcn: Int?,
    val band: String?,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?
)

class CellInfoProvider(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Read the current serving cell. Requires ACCESS_FINE_LOCATION, which the
     * app already holds while recording. Returns null when unavailable.
     */
    fun snapshot(): CellInfoSnapshot? {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val telephonyManager =
            appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return null
        val cells = try {
            telephonyManager.allCellInfo
        } catch (e: SecurityException) {
            null
        } ?: return null
        val serving = cells.firstOrNull { it.isRegistered } ?: return null
        val operator = telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() }
            ?: telephonyManager.simOperatorName?.takeIf { it.isNotBlank() }
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && serving is CellInfoNr ->
                fromNr(serving, operator)
            serving is CellInfoLte -> fromLte(serving, operator)
            serving is CellInfoWcdma -> fromWcdma(serving, operator)
            serving is CellInfoGsm -> fromGsm(serving, operator)
            else -> null
        }
    }

    private fun fromNr(info: CellInfoNr, operator: String?): CellInfoSnapshot {
        val id = info.cellIdentity as CellIdentityNr
        val ss = info.cellSignalStrength as CellSignalStrengthNr
        return CellInfoSnapshot(
            networkType = "NR",
            operatorName = operator,
            mcc = id.mccString,
            mnc = id.mncString,
            tac = id.tac.validOrNull(),
            pci = id.pci.validOrNull(),
            ci = id.nci.validOrNull(),
            arfcn = id.nrarfcn.validOrNull(),
            band = id.bands?.takeIf { it.isNotEmpty() }?.joinToString(",") { "n$it" },
            rsrp = ss.ssRsrp.validOrNull(),
            rsrq = ss.ssRsrq.validOrNull(),
            sinr = ss.ssSinr.validOrNull()
        )
    }

    private fun fromLte(info: CellInfoLte, operator: String?): CellInfoSnapshot {
        val id = info.cellIdentity as CellIdentityLte
        val ss = info.cellSignalStrength as CellSignalStrengthLte
        return CellInfoSnapshot(
            networkType = "LTE",
            operatorName = operator,
            mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
            mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null,
            tac = id.tac.validOrNull(),
            pci = id.pci.validOrNull(),
            ci = id.ci.validOrNull()?.toLong(),
            arfcn = id.earfcn.validOrNull(),
            band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                id.bands?.takeIf { it.isNotEmpty() }?.joinToString(",") { "B$it" }
            } else null,
            rsrp = ss.rsrp.validOrNull(),
            rsrq = ss.rsrq.validOrNull(),
            sinr = ss.rssnr.validOrNull()
        )
    }

    private fun fromWcdma(info: CellInfoWcdma, operator: String?): CellInfoSnapshot {
        val id = info.cellIdentity as CellIdentityWcdma
        return CellInfoSnapshot(
            networkType = "WCDMA",
            operatorName = operator,
            mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
            mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null,
            tac = id.lac.validOrNull(),
            pci = id.psc.validOrNull(),
            ci = id.cid.validOrNull()?.toLong(),
            arfcn = id.uarfcn.validOrNull(),
            band = null,
            rsrp = null,
            rsrq = null,
            sinr = null
        )
    }

    private fun fromGsm(info: CellInfoGsm, operator: String?): CellInfoSnapshot {
        val id = info.cellIdentity as CellIdentityGsm
        return CellInfoSnapshot(
            networkType = "GSM",
            operatorName = operator,
            mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
            mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null,
            tac = id.lac.validOrNull(),
            pci = null,
            ci = id.cid.validOrNull()?.toLong(),
            arfcn = id.arfcn.validOrNull(),
            band = null,
            rsrp = null,
            rsrq = null,
            sinr = null
        )
    }

    private fun Int.validOrNull(): Int? = if (this == Int.MAX_VALUE) null else this

    private fun Long.validOrNull(): Long? = if (this == Long.MAX_VALUE) null else this
}
