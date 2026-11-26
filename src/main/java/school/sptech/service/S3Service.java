package school.sptech.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import school.sptech.dao.AlertaDAO;
import school.sptech.model.DadosHardwareModelo;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

// Localiza o arquivo JSON mais recente no bucket e converte o conteúdo para o modelo de hardware.
public class S3Service
{
    private static final DateTimeFormatter FORMATO_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    private static String montarLog(String nivel, String bloco, String mensagem) {
        return "[" + LocalDateTime.now().format(FORMATO_LOG) + "] [" + nivel + "] [" + bloco + "] " + mensagem;
    }

    private static void info(String bloco, String mensagem) {
        System.out.println(montarLog("INFO", bloco, mensagem));
    }

    private static void warn(String bloco, String mensagem) {
        System.out.println(montarLog("WARN", bloco, mensagem));
    }

    private static void error(String bloco, String mensagem) {
        System.err.println(montarLog("ERRO", bloco, mensagem));
    }

    private static void section(String bloco, String titulo) {
        System.out.println();
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
        System.out.println(montarLog("BLOCO", bloco, titulo));
        System.out.println(montarLog("BLOCO", bloco, "--------------------------------------------------"));
    }

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
        this.objectMapper = new ObjectMapper();

        // Ignora propriedades desconhecidas do JSON sem falhar o parse
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Navega até o prefixo do equipamento, escolhe o JSON mais recente e o transforma em dados
    // tipados para que o restante do fluxo possa trabalhar com valores já estruturados.
    public DadosHardwareModelo lerUltimoArquivoJson(String bucketName, String empresa, String equipamento) {
        String prefixo = String.format("gold/%s/%s/", empresa, equipamento);

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefixo)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listReq);

        Optional<S3Object> ultimoObjeto = listResponse.contents().stream()
                .filter(objeto -> objeto.key().toLowerCase().endsWith(".json"))
                .max(Comparator.comparing(S3Object::lastModified));

        if (ultimoObjeto.isEmpty()) {
            warn("S3", "Nenhum arquivo JSON encontrado no caminho: " + prefixo);
            return null;
        }

        String keyMaisRecente = ultimoObjeto.get().key();
        info("S3", "Último arquivo selecionado no bucket: " + keyMaisRecente);

        // Antes de baixar o arquivo, confere no banco se algum alerta já foi gerado a partir dele.
        // Se já foi, retorna null para o fluxo não processar o mesmo arquivo de novo.
        try
        {
            AlertaDAO alertaDAO = new AlertaDAO();

            if (alertaDAO.arquivoJaLido(keyMaisRecente))
            {
                warn("S3", "Arquivo já processado anteriormente, ignorando: " + keyMaisRecente);
                return null;
            }
        }

        catch (Exception e)
        {
            error("S3", "Erro ao verificar no banco se o arquivo já foi lido: " + e.getMessage());
            return null;
        }


        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyMaisRecente)
                .build();

        try (ResponseInputStream<GetObjectResponse> s3InputStream = s3Client.getObject(getObjectRequest)) {

            DadosHardwareModelo dados = objectMapper.readValue(s3InputStream, DadosHardwareModelo.class);

            // Guarda de qual arquivo esses dados vieram para que o alerta criado depois saiba a origem.
            dados.setArquivoOrigem(keyMaisRecente);

            section("S3", "Arquivo lido com sucesso: " + keyMaisRecente);
            return dados;

        }

        catch (Exception e)
        {
            error("S3", "Erro ao ler ou converter o arquivo '" + keyMaisRecente + "': " + e.getMessage());
            e.printStackTrace();

            return null;
        }
    }
}
