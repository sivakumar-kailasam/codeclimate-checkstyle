#!/bin/sh

# use `make upgrade` to update this URL to the latest version
URL='https://github.com/checkstyle/checkstyle/releases/download/checkstyle-8.25/checkstyle-8.25-all.jar'

wget -O /usr/local/bin/checkstyle.jar $URL
