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


##  Input & Output

* **Input:** `.pcap` file (network traffic capture)
* **Output:** Filtered `.pcap` file + analytics report

---

##  Dataset

For testing, publicly available datasets were used:

👉 https://wiki.wireshark.org/SampleCaptures

These datasets include HTTP and TLS traffic, ideal for testing SNI-based application detection.

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

## SCREENSHOTS FOR BETTER UNDERSTANDING 
<img width="1920" height="1080" alt="Screenshot (459)" src="https://github.com/user-attachments/assets/7076fa9d-048c-4ea2-9e65-de8883024fd6" />
---
<img width="1920" height="1080" alt="Screenshot (460)" src="https://github.com/user-attachments/assets/f6e8d936-52fa-463d-94b9-f2fdfe453736" />
---
<img width="1920" height="1080" alt="Screenshot (461)" src="https://github.com/user-attachments/assets/0a0ce6e0-7ed2-4566-8a6d-100b7dbc4af9" />
---
<img width="1920" height="1080" alt="Screenshot (462)" src="https://github.com/user-attachments/assets/3bfb346a-7e17-4558-9618-d0d5fb13180b" />






