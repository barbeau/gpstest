#!/bin/bash

# Pushes updated English strings to Transifex so they can be translated by others
echo "Pushing new English strings to Transifex..."
# analyze current branch and react accordingly
pip install virtualenv
virtualenv ~/env
source ~/env/bin/activate
pip install transifex-client
sudo echo $'[https://www.transifex.com]\nhostname = https://www.transifex.com\nusername = '"$TRANSIFEX_USER"$'\npassword = '"$TRANSIFEX_PASSWORD"$'\ntoken = '"$TRANSIFEX_API_TOKEN"$'\n' > ~/.transifexrc
tx push -s
