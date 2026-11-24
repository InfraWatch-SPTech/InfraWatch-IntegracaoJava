package school.sptech.controller;

import school.sptech.dao.AlertaDAO;
import school.sptech.model.Alerta;

import java.util.List;

public class AlertaController
{
    private final AlertaDAO alertaDAO;
    //private final JiraController jiraController;

    public AlertaController() {
        this.alertaDAO = new AlertaDAO();
        //this.jiraController = new JiraController();
    }

    public void processarAlertas()
    {
        List<Alerta> alertas = alertaDAO.buscarTodos();

        for (Alerta alerta : alertas) {
            System.out.println(alerta);

            // Regra futura:
            // se o alerta atender uma condição, cria issue no Jira
        }
    }
}
