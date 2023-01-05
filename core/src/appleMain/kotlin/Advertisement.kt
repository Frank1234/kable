package com.juul.kable

import com.benasher44.uuid.Uuid
import platform.CoreBluetooth.CBAdvertisementDataIsConnectable
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBAdvertisementDataManufacturerDataKey
import platform.CoreBluetooth.CBAdvertisementDataServiceDataKey
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBAdvertisementDataTxPowerLevelKey
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID

public actual data class Advertisement(
    public actual val rssi: Int,
    val data: Map<String, Any>,
    internal val cbPeripheral: CBPeripheral,
) {

    val identifier: NSUUID
        get() = cbPeripheral.identifier

    public actual val name: String?
        get() = data[CBAdvertisementDataLocalNameKey] as? String

    /**
     * The [peripheralName] property may contain either advertising, or GAP name, dependent on various conditions:
     *
     * | Condition(s)                               | `gapName` value                                      |
     * |--------------------------------------------|------------------------------------------------------|
     * | Initial peripheral discovery (active scan) | `advertisementData(CBAdvertisementDataLocalNameKey)` |
     * | Connected to peripheral                    | GAP name                                             |
     * | Subsequent discovery (disconnected)        | GAP name (from cache)                                |
     *
     * https://developer.apple.com/forums/thread/72343
     */
    public actual val peripheralName: String?
        get() = cbPeripheral.name

    /** https://developer.apple.com/documentation/corebluetooth/cbadvertisementdataisconnectable */
    public actual val isConnectable: Boolean?
        get() = data[CBAdvertisementDataIsConnectable] as? Boolean

    public actual val txPower: Int?
        get() = (data[CBAdvertisementDataTxPowerLevelKey] as? NSNumber)?.intValue

    actual val uuids: List<Uuid>
        get() = (data[CBAdvertisementDataServiceUUIDsKey] as? List<CBUUID>)?.map { it.toUuid() } ?: emptyList()

    public actual fun serviceData(uuid: Uuid): ByteArray? =
        serviceDataAsNSData(uuid)?.toByteArray()

    public fun serviceDataAsNSData(uuid: Uuid): NSData? =
        (data[CBAdvertisementDataServiceDataKey] as? Map<CBUUID, NSData>)?.get(uuid.toCBUUID())

    public actual fun manufacturerData(companyIdentifierCode: Int): ByteArray? =
        manufacturerData?.takeIf { (it.code == companyIdentifierCode) }?.data

    public actual val manufacturerData: ManufacturerData?
        get() = manufacturerDataAsNSData?.toManufacturerData()

    public fun manufacturerDataAsNSData(companyIdentifierCode: Int): NSData? =
        manufacturerData(companyIdentifierCode)?.toNSData()

    public val manufacturerDataAsNSData: NSData?
        get() = data[CBAdvertisementDataManufacturerDataKey] as? NSData

    override fun toString(): String =
        "Advertisement(name=$name, cbPeripheral=$cbPeripheral, rssi=$rssi, txPower=$txPower)"
}

internal fun NSData.toManufacturerData(): ManufacturerData? = toByteArray().toManufacturerData()

private fun ByteArray.toManufacturerData(): ManufacturerData? =
    takeIf { size >= 2 }?.getShortAt(0)?.let { code ->
        ManufacturerData(
            code.toInt(),
            if (size > 2) slice(2 until size).toByteArray() else byteArrayOf(),
        )
    }