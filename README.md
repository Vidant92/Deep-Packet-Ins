# Java Deep Packet Inspection (DPI) Engine

A high-performance, multi-threaded **Deep Packet Inspection (DPI) Engine** built using **pure Java** (no external packet libraries).
This project analyzes raw `.pcap` network traffic and performs **Layer 7 inspection** to detect real-world applications like YouTube, Instagram, Twitter, etc., and applies filtering rules.

---

##  Project Overview

Traditional firewalls rely on IPs and ports, which are often insufficient due to encryption and CDNs.
This project goes deeper by analyzing the **actual packet payload (Layer 7)* to identify applications and enforce rules.

---

##  Features

*  **PCAP File Parsing** – Reads raw `.pcap` files without third-party libraries
*  **Layer 7 Inspection** – Extracts HTTP Host & TLS SNI
*  **Application Blocking** – Block apps like YouTube, TikTok
*  **Multi-threaded Processing** – High-performance packet handling
*  **Load Balancing** – Distributes packets across worker threads
* **Detailed Analytics** – Application breakdown, domain stats, packet metrics
* **Connection Tracking** – Based on Five-Tuple (IP, Port, Protocol)

---

##  Architecture

```
PCAP Reader → Load Balancer → FastPath Processors → DPI Engine → Output
```

* **PcapReader** → Reads raw packets
* **LoadBalancer** → Distributes packets across threads
* **FastPathProcessor** → Processes packets concurrently
* **PacketParser** → Extracts headers & payload
* **SNIExtractor** → Identifies domain names
* **RuleManager** → Applies blocking rules
* **ConnectionTracker** → Tracks active connections

---

##  Core Concepts Used

* OSI Model & TCP/IP Stack
* Ethernet, IPv4, TCP/UDP Parsing
* Bitwise Operations & Byte Manipulation
* Endianness Handling (Little vs Big Endian)
* Java Multithreading (`BlockingQueue`, worker threads)
* Load Balancing using hashing

---

##  Sample Output

```
Total Packets: 77
TCP Packets: 73
UDP Packets: 4

Forwarded: 76
Dropped: 1

BLOCKED packet: APP YouTube

APPLICATION BREAKDOWN:
YouTube       1
Twitter       1
Instagram     1
...
```

---

##  How to Run

### 1️⃣ Navigate to Project Folder

```
cd C:\Users\PC\Desktop\Packet_analyzer\dpi_engine_java
```

### 2️⃣ Build the Project

```
mvn clean package
```

### 3️⃣ Run the Engine

```
java -jar target\dpi-engine-1.0-SNAPSHOT-jar-with-dependencies.jar test_dpi.pcap output.pcap
```

###  Block Specific App

```
java -jar target\dpi-engine-1.0-SNAPSHOT-jar-with-dependencies.jar test_dpi.pcap output.pcap --block-app YouTube
```

---

##  Input & Output

* **Input:** `.pcap` file (network traffic capture)
* **Output:** Filtered `.pcap` file + analytics report

---

##  Dataset

For testing, publicly available datasets were used:

👉 https://wiki.wireshark.org/SampleCaptures

These datasets include HTTP and TLS traffic, ideal for testing SNI-based application detection.

---

## ⚠️ Limitations

* Cannot inspect fully encrypted payloads (HTTPS beyond handshake)
* Limited support for QUIC / HTTP3
* Works on offline PCAP files (not real-time yet)

---

## 🚀 Future Enhancements

*  Real-time packet sniffing
*  GUI dashboard (traffic visualization)
*  Machine learning-based traffic classification
*  Advanced TLS parsing

---

##  Use Cases

* Network Monitoring
* Enterprise Firewall Systems
* Parental Control
* Cybersecurity Research
* Traffic Analytics

---

##  Author

**Vidant Bhardwaj**

* LinkedIn: https://www.linkedin.com/in/vidant-bhardwaj-b64970380/

---

## ⭐ Support

If you found this project useful, consider giving it a ⭐ on GitHub!

Happy learning! 🚀
