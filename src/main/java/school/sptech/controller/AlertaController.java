package school.sptech.controller;

import school.sptech.dao.AlertaDAO;
import school.sptech.model.Alerta;

import java.util.List;

// Mantido como ponto de apoio para operações futuras relacionadas a alertas.
public class AlertaController
{
    private final AlertaDAO alertaDAO;

    public AlertaController()
    {
        // Mantém o acesso ao DAO encapsulado no controller, preservando o padrão usado pelo projeto
        // mesmo que esta classe ainda esteja como ponto de expansão para fluxos futuros.
        this.alertaDAO = new AlertaDAO();
    }
}
