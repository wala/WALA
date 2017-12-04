#!/bin/bash

body='{
"request": {
"branch":"master"
}}'

curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $1" \
   -d "$body" \
   https://api.travis-ci.org/repo/wala%2FClient/requests

curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $1" \
   -d "$body" \
   https://api.travis-ci.org/repo/wala%2FWALA-Mobile/requests

curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $1" \
   -d "$body" \
   https://api.travis-ci.org/repo/wala%2FMemSAT/requests

curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $1" \
   -d "$body" \
   https://api.travis-ci.org/repo/april1989%2FIncremental_Points_to_Analysis/requests

curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $1" \
   -d "$body" \
   https://api.travis-ci.org/repo/SunghoLee%2FHybriDroid/requests

