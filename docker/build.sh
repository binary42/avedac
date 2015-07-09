#!/bin/bash
set -x
tmp=tmp.$RANDOM
eval "sed 's/SALIENCY_SVN_PASSWORD/$1/g' Dockerfile > $tmp"
docker build -t danellecline/avedac $tmp
rm $tmp 
