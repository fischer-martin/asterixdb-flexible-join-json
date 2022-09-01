#!/usr/bin/bash -e

pushd $(dirname "$0") > /dev/null

#curl --fail-with-body -v --data-urlencode 'statement=DROP DATAVERSE SpotifyDiscogs IF EXISTS; CREATE DATAVERSE SpotifyDiscogs; USE SpotifyDiscogs; CREATE TYPE SpotifyType AS {id: string}; CREATE TYPE DiscogsType AS {id: int}; CREATE DATASET Spotify(SpotifyType) PRIMARY KEY id; CREATE DATASET Discogs(DiscogsType) PRIMARY KEY id; LOAD DATASET Spotify USING localfs((`path`=`localhost:///media/Shared/Martin/Documents/Uni/Master_Informatik_Salzburg/22SS/Masterarbeit/music-metadata-scrapers/spotify_short.json`),(`format`=`json`)); LOAD DATASET Discogs USING localfs((`path`=`localhost:///media/Shared/Martin/Documents/Uni/Master_Informatik_Salzburg/22SS/Masterarbeit/music-metadata-scrapers/discogs_short.json`),(`format`=`json`));' http://localhost:19004/query/service
curl --fail-with-body -v --data-urlencode 'statement=DROP DATAVERSE SpotifyDiscogs IF EXISTS; CREATE DATAVERSE SpotifyDiscogs; USE SpotifyDiscogs; CREATE TYPE SpotifyType AS {id: string}; CREATE TYPE DiscogsType AS {id: int}; CREATE DATASET Spotify(SpotifyType) PRIMARY KEY id; CREATE DATASET Discogs(DiscogsType) PRIMARY KEY id; LOAD DATASET Spotify USING localfs((`path`=`localhost:///media/Shared/Martin/Documents/Uni/Master_Informatik_Salzburg/22SS/Masterarbeit/asterixdb-flexible-join-json/test-data/test1.adm`),(`format`=`adm`)); LOAD DATASET Discogs USING localfs((`path`=`localhost:///media/Shared/Martin/Documents/Uni/Master_Informatik_Salzburg/22SS/Masterarbeit/asterixdb-flexible-join-json/test-data/test2.adm`),(`format`=`adm`));' http://localhost:19004/query/service

rm -f flexiblejoin.jar.zip
mvn package
zip -j flexiblejoin.jar.zip target/*.jar

curl --fail-with-body -v -u admin:adminpw -X POST -F 'data=@flexiblejoin.jar.zip' -F 'type=java' localhost:19005/admin/udf/SpotifyDiscogs/flexiblejoins

curl --fail-with-body -v --data-urlencode 'statement=USE SpotifyDiscogs; CREATE JOIN jsonjoin_interval(a, b, threshold: double) AS "jsonjoin.JsonJoin" AT flexiblejoins;' http://localhost:19004/query/service

popd > /dev/null
