#!/bin/bash

DBNAME="lor"
UPDATES_DIR="updates"

#==============================================================================
PSQL="psql -q -t -d ${DBNAME} "

isReadyForUpdates(){
    haveUpdateTable=`${PSQL} -c "SELECT count(tablename) FROM pg_tables WHERE tablename = 'db_updates'"`
    if [ ${haveUpdateTable} -ne 0 ]; then
        return
    fi
    ${PSQL} -f init_for_updates.sql
}

# -----------------------------------------------------------------------------

check_update() {
    index=$1; shift
    ${PSQL} -c "SELECT count(id) FROM db_updates WHERE id='${index}'"
}

# -----------------------------------------------------------------------------

apply_update() {
    fileName=$1; shift
    ${PSQL} -f "${fileName}"
}

#==============================================================================

isReadyForUpdates

find ${UPDATES_DIR} -maxdepth 1 -name "up*\.sql" -type f | sort | while read i; do
    index=`echo "$i" | sed "s/.*\/up\([0-9]*\).*/\1/g"`

    if [ 0 -eq `check_update $index` ]; then
        apply_update "$i"
    fi
done
