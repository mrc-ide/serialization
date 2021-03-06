#!/usr/bin/env bash

function git_dirty {
  [[ $(git diff --shortstat 2> /dev/null | tail -n1) != "" ]] && echo "*"
}

if [[ $(git diff --shortstat 2> /dev/null | tail -n1) != "" ]] && echo "*" = "*"; then
    echo "git not clean, commit changes first"
    exit 1
fi

echo "-------------------------------------------"
echo 'Creating maven artifacts'
./gradlew publish

git add .
git commit -m "publish"

echo "-------------------------------------------"
echo 'Pushing to mvn-repo'
git subtree push --prefix repo origin mvn-repo
