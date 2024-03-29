#!/bin/bash

# Pushes updated English strings to Transifex so they can be translated by others
echo "Pushing new English strings to Transifex..."
# analyze current branch and react accordingly
pip install virtualenv
virtualenv ~/env
source ~/env/bin/activate
pip install transifex-client
export TX_TOKEN=$TRANSIFEX_API_TOKEN
tx push -s --force --no-interactive