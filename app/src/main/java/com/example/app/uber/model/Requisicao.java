package com.example.app.uber.model;

import android.util.Log;

import com.example.app.uber.firebase.FireBase;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class Requisicao {

    public static final String STATUS_AGUARDANDO = "aguardando";
    public static final String STATUS_A_CAMINHO = "acaminho";
    public static final String STATUS_VIAGEM = "viagem";
    public static final String STATUS_FINALIZADA = "finalizada";
    public static final String STATUS_ENCERRADA = "encerrada";
    public static final String STATUS_CANCELADA = "cancelada";

    private String id;
    private String status;
    private Usuario passageiro;
    private Usuario motorista;
    private Destino destino;

    public Requisicao() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Usuario getPassageiro() {
        return passageiro;
    }

    public void setPassageiro(Usuario passageiro) {
        this.passageiro = passageiro;
    }

    public Usuario getMotorista() {
        return motorista;
    }

    public void setMotorista(Usuario motorista) {
        this.motorista = motorista;
    }

    public Destino getDestino() {
        return destino;
    }

    public void setDestino(Destino destino) {
        this.destino = destino;
    }

    public void salvar() {

        DatabaseReference databaseReference = FireBase.getFirebaseDatabase();
        DatabaseReference requisicoes = databaseReference.child("requisicoes");

        String idRequisicao = requisicoes.push().getKey();
        setId(idRequisicao);
        requisicoes.child(getId()).setValue(this);
    }

    public void atualizar() {
        DatabaseReference databaseReference = FireBase.getFirebaseDatabase();
        DatabaseReference requisicoes = databaseReference.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId());

        Map<String, Object> objeto = new HashMap<String, Object>();
        objeto.put("motorista", getMotorista());
        objeto.put("status", getStatus());
        requisicao.updateChildren(objeto);
    }

    public void atualizarStatus(){
        DatabaseReference databaseReference = FireBase.getFirebaseDatabase();
        DatabaseReference requisicoes = databaseReference.child("requisicoes");
        Log.i("teste", "id: " + getId());
        DatabaseReference requisicao = requisicoes.child(getId());

        Map<String,Object> objectMap = new HashMap<>();
        objectMap.put("status",getStatus());
        requisicao.updateChildren(objectMap);
    }

    public void atualizarLocalizacaoMotorista() {
        DatabaseReference databaseReference = FireBase.getFirebaseDatabase();
        DatabaseReference requisicoes = databaseReference.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId()).child("motorista");

        Map<String, Object> objeto = new HashMap<String, Object>();
        objeto.put("latitude", getMotorista().getLatitude());
        objeto.put("longitude", getMotorista().getLongitude());
        requisicao.updateChildren(objeto);
    }


}
