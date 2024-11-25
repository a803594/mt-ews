### Обновление сертификата

Создать сертификат командой:
```sh
keytool -genkeypair \
-alias localhost \
-keyalg RSA \
-keysize 2048 \
-validity 365 \
-keystore localhost.jks \
-dname "CN=localhost" \
-ext san=dns:localhost \
-storepass 123456 \
-keypass 123456
```

Полученный файл localhost.jks положить с заменой в исходный код mt-ews по пути src/main/resource/keys/