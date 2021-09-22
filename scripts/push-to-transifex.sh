#!/bin/bash

# Pushes updated English strings to Transifex so they can be translated by others
echo "Pushing new English strings to Transifex..."
# analyze current branch and react accordingly
pip install virtualenv
virtualenv ~/env
source ~/env/bin/activate
pip install transifex-client
tx push -s --token=$TRANSIFEX_API_TOKEN --force --no-interactive