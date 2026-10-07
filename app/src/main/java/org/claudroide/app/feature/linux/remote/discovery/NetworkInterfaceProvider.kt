package org.claudroide.app.feature.linux.remote.discovery

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

/**
 * Information about a local network interface used for peer discovery.
 */
data class InterfaceInfo(
    val name: String,
    val displayName: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val addresses: List<InetAddress>
)

/**
 * Abstraction for network interface discovery to allow unit testing on JVM without physical hardware.
 */
interface NetworkInterfaceProvider {
    fun getInterfaces(): List<InterfaceInfo>
}

class SystemNetworkInterfaceProvider : NetworkInterfaceProvider {
    override fun getInterfaces(): List<InterfaceInfo> {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            Collections.list(interfaces).map { nif ->
                InterfaceInfo(
                    name = nif.name,
                    displayName = nif.displayName ?: nif.name,
                    isUp = try { nif.isUp } catch (_: Throwable) { false },
                    isLoopback = try { nif.isLoopback } catch (_: Throwable) { false },
                    addresses = Collections.list(nif.inetAddresses)
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
