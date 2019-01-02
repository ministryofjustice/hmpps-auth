#!/bin/bash

version=${1?No version specified}
source="https://raw.githubusercontent.com/alphagov/govuk-frontend/v$version/dist"

files="govuk-frontend-$version.min.css
       govuk-frontend-$version.min.js
       govuk-frontend-ie8-$version.min.css"
fonts="bold-a2452cb66f-v1.woff2
       bold-f38c792ac2-v1.woff
       bold-fb2676462a-v1.eot
       bold-tabular-357fdfbcc3-v1.eot
       bold-tabular-784c21afb8-v1.woff
       bold-tabular-b89238d840-v1.woff2
       light-2c037cf7e1-v1.eot
       light-458f8ea81c-v1.woff
       light-f38ad40456-v1.woff2
       light-tabular-498ea8ffe2-v1.eot
       light-tabular-62cc6f0a28-v1.woff
       light-tabular-851b10ccdd-v1.woff2"
images="favicon.ico
        govuk-apple-touch-icon-152x152.png
        govuk-apple-touch-icon-167x167.png
        govuk-apple-touch-icon-180x180.png
        govuk-apple-touch-icon.png
        govuk-crest-2x.png
        govuk-crest.png
        govuk-logotype-crown.png
        govuk-mask-icon.svg
        govuk-opengraph-image.png
        icon-arrow-left.png
        icon-important.png
        icon-pointer-2x.png
        icon-pointer.png"

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
