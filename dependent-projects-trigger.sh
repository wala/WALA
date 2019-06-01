#!/bin/bash

authToken=$1

triggerTravis() {
  body='{ "request": { "branch":"master" } }'
  travisURL=https://api.travis-ci.org/repo/${1}/requests

  curl -s -X POST \
       -H "Content-Type: application/json" \
       -H "Accept: application/json" \
       -H "Travis-API-Version: 3" \
       -H "Authorization: token $authToken" \
       -d "$body" \
       "$travisURL"
}

triggerTravis wala%2FClient
triggerTravis wala%2FWALA-Mobile
triggerTravis wala%2FMemSAT
triggerTravis april1989%2FIncremental_Points_to_Analysis
triggerTravis SunghoLee%2FHybriDroid
triggerTravis wala%2FML
