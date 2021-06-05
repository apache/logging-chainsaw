#!/bin/bash

# script to generate .asc, .md5, and .sha512 files for built artifacts

if [[ $# -ne 1 ]]
then
    echo "Usage: $0 gpg-key-id"
    exit 1
fi

GPG_KEY="$1"

cd target
for f in apache-chainsaw-*-bin.* apache-chainsaw-*-standalone.* apache-chainsaw-source*.*
do
    gpg --default-key="$GPG_KEY" --sign --detach-sign --armor "$f"
    shasum -a 512 "$f" >"$f.sha512"
done
