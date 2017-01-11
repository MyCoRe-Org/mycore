#!/usr/bin/env bash

#$1 should be the BranchID
#$2 should be the repository e.g. https://github.com/MyCoRe-Travis/test_artifacts.git

git clone $2 ./autodeploy
cd ./autodeploy
git branch $1
git checkout $1
printf "Travis: https://travis-ci.org/MyCoRe-Org/mycore/builds/$TRAVIS_BUILD_ID \n\nMycore-Pull: https://github.com/MyCoRe-Org/mycore/pull/$TRAVIS_PULL_REQUEST \n\nCommit: https://github.com/MyCoRe-Org/mycore/commit/$TRAVIS_COMMIT" > README.md
cd ../

find . \( -path "*/surefire-reports/*" -o -path "*/failsafe-reports/*" -o -path "*/screenshots/*" -o -name "*error*.log" \) -print0 | rsync -av --files-from=- --from0 ./ autodeploy

cd ./autodeploy
git add .
git commit -m "adding test results"
git push -f --set-upstream origin $1

SAVE=20

FIRST=$(( TRAVIS_BUILD_NUMBER - SAVE ))
PROTECT=$(seq $FIRST $TRAVIS_BUILD_NUMBER)

eval "$(git for-each-ref --shell --format='git push origin --delete %(refname)' refs/remotes/origin|grep -v $(echo "$PROTECT" |sed -e 's|\(.*\)|refs/remotes/origin/\1|g'|xargs -I repl echo -n repl"\\|" && echo -n 'refs/remotes/origin/HEAD\|refs/remotes/origin/master')|sed -e 's|refs/remotes/origin/||')"

if [ "$TRAVIS_PULL_REQUEST" != "false" ]
then 
	curl -H "Authorization: token $GITHUB_TOKEN" -X POST -d "{\"body\":\"Something gone wrong on $TRAVIS_COMMIT! \n Please Check https://github.com/MyCoRe-Travis/test_artifacts/tree/$1\"}" https://api.github.com/repos/MyCoRe-Org/mycore/issues/$TRAVIS_PULL_REQUEST/comments
fi
