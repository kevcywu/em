@echo off
TITLE ~Clean Revision 988 Files~ Made by Rigged of RaGEZONE Forums (login)
COLOR 02
set CLASSPATH=.;dist\*;lib\*
java -Xmx500m -Dnet.sf.odinms.recvops=recvops.properties -Dnet.sf.odinms.sendops=sendops.properties -Dnet.sf.odinms.wzpath=wz\ -Dnet.sf.odinms.login.config=login.properties -Dnet.sf.odinms.listwz=true -Djavax.net.ssl.keyStore=login.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=login.truststore -Djavax.net.ssl.trustStorePassword=passwd net.sf.odinms.net.login.LoginServer
pause