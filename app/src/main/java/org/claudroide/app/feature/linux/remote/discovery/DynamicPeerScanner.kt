package org.claudroide.app.feature.linux.remote.discovery

import org.claudroide.app.feature.linux.remote.ConnectionBearer
import org.claudroide.app.feature.linux.remote.RemoteHost
import java.net.Inet4Address

class DynamicPeerScanner(
    private val interfaceProvider: NetworkInterfaceProvider = SystemNetworkInterfaceProvider(),
    private val arpReader: ArpTableReader = ProcNetArpReader()
) {

    /**
     * Determines which interface prefixes correspond to a given bearer.
     */
    fun interfacePrefixesForBearer(bearer: ConnectionBearer): List<String> {
        return when (bearer) {
            ConnectionBearer.WLAN_HOTSPOT -> listOf("ap", "softap", "wlan")
            ConnectionBearer.USB_TETHERING -> listOf("rndis", "usb", "ncm", "eth")
            ConnectionBearer.BLUETOOTH_PAN -> listOf("bt-pan", "bnep")
        }
    }

    /**
     * Discovers remote hosts dynamically based on active network interfaces and ARP table entries.
     * No hardcoded static IP addresses are used.
     */
    fun scanPeers(bearer: ConnectionBearer): List<RemoteHost> {
        val prefixes = interfacePrefixesForBearer(bearer)
        val interfaces = interfaceProvider.getInterfaces().filter { iface ->
            iface.isUp && !iface.isLoopback && prefixes.any { iface.name.lowercase().startsWith(it) }
        }

        if (interfaces.isEmpty()) {
            return emptyList()
        }

        val interfaceNames = interfaces.map { it.name }.toSet()
        val arpEntries = arpReader.readEntries().filter { entry ->
            entry.device in interfaceNames && entry.isComplete
        }

        val discovered = mutableListOf<RemoteHost>()

        // 1. Add peers detected in the kernel ARP table (dynamically connected devices)
        for (arp in arpEntries) {
            discovered.add(
                RemoteHost(
                    ipAddress = arp.ipAddress,
                    bearer = bearer,
                    sshPort = 22,
                    vncPort = 5900,
                    isReachable = false,
                    macAddress = arp.hwAddress
                )
            )
        }

        // 2. If no ARP entry has been populated yet, extract dynamic subnet from the active interface
        if (discovered.isEmpty()) {
            for (iface in interfaces) {
                val ipv4 = iface.addresses.filterIsInstance<Inet4Address>().firstOrNull() ?: continue
                val hostBytes = ipv4.address
                // Derive peer address on /24 subnet dynamically: if local is .1, standard client is .2 or gateway
                val octets = hostBytes.map { it.toInt() and 0xFF }
                if (octets.size == 4) {
                    val candidateLastOctet = if (octets[3] == 1) 2 else 1
                    val candidateIp = "${octets[0]}.${octets[1]}.${octets[2]}.$candidateLastOctet"
                    discovered.add(
                        RemoteHost(
                            ipAddress = candidateIp,
                            bearer = bearer,
                            sshPort = 22,
                            vncPort = 5900,
                            isReachable = false,
                            hostname = "dynamic-peer-${iface.name}"
                        )
                    )
                }
            }
        }

        return discovered.distinctBy { it.ipAddress }
    }
}
