package com.atmate.portal.integration.atmateintegration.beans;

import lombok.Data;

@Data
public class ClientDetailsBean {
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
    private String data_cessacao;
    private String email_alt;
    private String telefone_alt;
}
