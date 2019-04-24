#!/bin/bash

# Pushes updated English strings to Transifex so they can be translated by others
# Only pushes if the Travis build isn't a pull request and we're on the master branch

if [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]

then
  echo "Pushing new English strings to Transifex..."
  # analyze current branch and react accordingly
  pip install virtualenv
  virtualenv ~/env
  source ~/env/bin/activate
  pip install transifex-client
  sudo echo $'[https://www.transifex.com]\nhostname = https://www.transifex.com\nusername = '"$TRANSIFEX_USER"$'\npassword = '"$TRANSIFEX_PASSWORD"$'\ntoken = '"$TRANSIFEX_API_TOKEN"$'\n' > ~/.transifexrc
  tx push -s
else
  echo "This isn't master branch or is a pull requeest - don't push strings to Transifex."
fi