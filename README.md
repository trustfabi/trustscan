#TrustScan# – Lightweight Java Port Scanner
TrustScan is a fast and lightweight command-line port scanner written in Java. It allows users to scan target hosts for open TCP ports within a defined range. Designed to be simple to use, easy to install, and cross-platform compatible, TrustScan is ideal for developers, sysadmins, or cybersecurity enthusiasts who need a no-nonsense port scanning utility.

🔍 Features
🚀 Fast multi-threaded TCP port scanning

🎯 Target IP and port range input via CLI

💾 Optional output as JSON for further processing

🖥️ Clean terminal output with professional ASCII banner

📦 Built with Gradle, runnable as a single .jar

🛠️ One-command setup with symlink installer (trustscan command globally available)

📦 Installation
Clone the repository:
git clone https://github.com/yourusername/trustscan.git
cd trustscan
./gradlew build
./trustscan.sh install
Now you can run TrustScan globally from anywhere:

trustscan <target-ip> --start <port> --end <port>
Example:
trustscan 192.168.1.1 --start 1 --end 1024

🧰 Requirements
Java 17 or higher

Unix-like system (Linux/macOS) for installer script (trustscan.sh)

Gradle (or use included wrapper ./gradlew)

