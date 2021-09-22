#!/bin/bash

# Pushes updated English strings to Transifex so they can be translated by others
echo "Pushing new English strings to Transifex..."
# analyze current branch and react accordingly
pip install virtualenv
virtualenv ~/env
source ~/env/bin/activate
pip install transifex-client
# Write .transifexrc file
cat > ~/.transifexrc <<EOF
[https://www.transifex.com]
hostname = https://www.transifex.com
token = $TRANSIFEX_API_TOKEN
EOF
tx push -s --force --no-interactive