#!/usr/bin/env bash
set -e

cd /root/board-backend
bash ./makeback.sh
cd /root/board-frontend
bash ./makefrnt.sh
