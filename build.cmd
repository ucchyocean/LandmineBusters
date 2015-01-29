@echo off
pushd %~dp0

echo ----- clean up -----
if exist release rmdir /s /q release
if errorlevel 1 goto errorend
mkdir release
if errorlevel 1 goto errorend

echo ----- making release package for BukkitDev -----
move /y pom.xml pom.xml.backup
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang en
call mvn clean deploy
if errorlevel 1 goto errorend
pushd target
ren LandmineBusters-*-dist.zip LandmineBusters-*-en.zip
popd
move /y target\LandmineBusters-*-en.zip release\

echo ----- making release package for Japan User Forum -----
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang ja
call mvn clean javadoc:jar source:jar deploy
if errorlevel 1 goto errorend
pushd target
ren LandmineBusters-*-dist.zip LandmineBusters-*-ja.zip
popd
move /y target\LandmineBusters-*-ja.zip release\

echo ----- finalize -----
move /y pom.xml.backup pom.xml

echo ===== Succeeded! =====
goto end

:errorend
echo ===== Failed! Please check the build error logs =====

:end
popd
