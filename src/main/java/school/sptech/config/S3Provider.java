package school.sptech.config;

import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// Responsável por centralizar a criação do cliente AWS S3 e a leitura do nome do bucket.
public class S3Provider
{
    private final Dotenv dotenv = Dotenv.load();

    // Monta o cliente com credenciais temporárias vindas do .env, mantendo a configuração de
    // infraestrutura isolada da leitura de arquivos e das regras de negócio.
    public S3Client criarClienteS3()
    {
        String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String sessionToken = dotenv.get("AWS_SESSION_TOKEN");
        Region region = Region.of(dotenv.get("AWS_REGION"));

        AwsCredentials credentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);

        return S3Client.builder().region(region).credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
    }

    public String getNomeBucket() {
        return dotenv.get("AWS_BUCKET_NAME");
    }
}
