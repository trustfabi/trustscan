# TrustScan

Professioneller Portscanner mit Banner-Grabbing und IP-Range-Scan.

## Installation

```bash
git clone https://github.com/deinuser/trustscan.git
cd trustscan
chmod +x trustscan.sh
./gradlew build
sudo ln -s $(pwd)/trustscan.sh /usr/local/bin/trustscan
