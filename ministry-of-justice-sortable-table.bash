#!/bin/bash
workingdir="moj-sortable-table"
rm -r $workingdir 2>/dev/null
mkdir $workingdir 2>/dev/null
cd $workingdir
npm init -y
npm install govuk-frontend @ministryofjustice/frontend sass uglify-js jquery --save
version=$(grep ministryofjustice/frontend package.json | awk '{print $2}' | sed 's/[\^,"]//g')
jquery=$(grep jquery package.json | awk '{print $2}' | sed 's/[\^,"]//g')

sed '1s#^#@import "govuk/settings/_colours-applied.scss";\n#'  node_modules/@ministryofjustice/frontend/moj/components/sortable-table/_sortable-table.scss | node_modules/sass/sass.js --stdin -I node_modules/govuk-frontend | grep -v '^@charset' > ../src/main/resources/static/css/moj-sortable-table.$version.css

node_modules/uglify-js/bin/uglifyjs --compress -- node_modules/@ministryofjustice/frontend/moj/components/sortable-table/sortable-table.js > ../src/main/resources/static/js/moj-sortable-table.$version.min.js

cp node_modules/jquery/dist/jquery.min.js ../src/main/resources/static/js/jquery.$jquery.min.js
