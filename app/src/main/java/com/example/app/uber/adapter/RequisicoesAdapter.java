package com.example.app.uber.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.uber.R;
import com.example.app.uber.helper.Local;
import com.example.app.uber.model.Requisicao;
import com.example.app.uber.model.Usuario;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class RequisicoesAdapter extends RecyclerView.Adapter<RequisicoesAdapter.MyviewHolder> {
    private final List<Requisicao> requisicaoList;
    private final Context context;
    private final Usuario motorista;

    public RequisicoesAdapter(List<Requisicao> requisicaoList, Context context, Usuario motorista) {
        this.requisicaoList = requisicaoList;
        this.context = context;
        this.motorista = motorista;
    }

    @NonNull
    @Override
    public MyviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_requisicoes, parent, false);
        return new MyviewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyviewHolder holder, int position) {
        Requisicao requisicao = requisicaoList.get(position);
        Usuario passageiro = requisicao.getPassageiro();

        if (passageiro != null) {
            holder.nome.setText(passageiro.getNome());
            if(motorista!=null){
                LatLng localMotorista = new LatLng(
                        Double.parseDouble(motorista.getLatitude()),
                        Double.parseDouble(motorista.getLongitude())
                );
                LatLng localPassageiro = new LatLng(
                        Double.parseDouble(passageiro.getLatitude()),
                        Double.parseDouble(passageiro.getLongitude())
                );
                float distancia = Local.calcularDistancia(localPassageiro, localMotorista);
                String distanciaFormatada = Local.formatarDistancia(distancia);
                holder.distancia.setText(distanciaFormatada + " - aproximadamente");
            }
        }
    }

    @Override
    public int getItemCount() {
        return requisicaoList.size();
    }

    public static class MyviewHolder extends RecyclerView.ViewHolder {

        TextView nome, distancia;

        public MyviewHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.textRequisicaoNome);
            distancia = itemView.findViewById(R.id.textRequisicaoDistancia);
        }
    }
}
