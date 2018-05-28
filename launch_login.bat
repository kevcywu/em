@echo off
TITLE Login
COLOR 02
set CLASSPATH=.;dist\*
java -Xmx500m -Dnet.sf.odinms.listwz=false -Djavax.net.ssl.keyStore=login.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=login.truststore -Djavax.net.ssl.trustStorePassword=passwd net.login.LoginServer
pause