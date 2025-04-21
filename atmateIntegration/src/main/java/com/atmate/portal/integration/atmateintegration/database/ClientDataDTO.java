package com.atmate.portal.integration.atmateintegration.database;

import lombok.Data;

@Data
public class ClientDataDTO {
    private String nif;
    private String nome;
    private String data_nascimento;
    private String sexo;
    private String distrito;
    private String concelho;
    private String freguesia;
    private String pais_residencia;
    private String nacionalidade;
    private String morada;
    private String codigo_postal;
    private String telefone;
    private String email;
    private boolean atividade_exercida_encontrada;
}
