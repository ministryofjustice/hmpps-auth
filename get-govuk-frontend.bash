#!/bin/bash

version=${1?No version specified}
source="https://raw.githubusercontent.com/alphagov/govuk-frontend/v$version/dist"

files="govuk-frontend-$version.min.css
       govuk-frontend-$version.min.js
       govuk-frontend-ie8-$version.min.css"
fonts="bold-affa96571d-v2.woff
       bold-b542beb274-v2.woff2
       light-94a07e06a1-v2.woff2
       light-f591b13f7d-v2.woff"
images="favicon.ico
        govuk-apple-touch-icon-152x152.png
        govuk-apple-touch-icon-167x167.png
        govuk-apple-touch-icon-180x180.png
        govuk-apple-touch-icon.png
        govuk-crest-2x.png
        govuk-crest.png
        govuk-logotype-crown.png
        govuk-mask-icon.svg
        govuk-opengraph-image.png"

for file in $(echo $files); do
  file_type=${file##*.}
  echo "Requesting $file of type $file_type"
  # need to change asset paths as we are running under auth domain
  curl -s "$source/$file" | sed 's#/assets/#/auth/#g' > "src/main/resources/static/$file_type/$file"
done

cd "src/main/resources/static/fonts" 
for font in $(echo $fonts); do
  echo "Requesting $font"
  curl -s --remote-name "$source/assets/fonts/$font"
done
cd - > /dev/null

cd "src/main/resources/static/images" 
for image in $(echo $images); do
  echo "Requesting $image"
  curl -s --remote-name "$source/assets/images/$image"
done
