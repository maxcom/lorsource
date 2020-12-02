#!/bin/sh
createuser -U maxcom linuxweb
createuser -U maxcom jamwiki
psql -U maxcom -c "ALTER USER linuxweb PASSWORD 'linuxweb'" template1
psql -U maxcom -f /opt/demo.db lor
