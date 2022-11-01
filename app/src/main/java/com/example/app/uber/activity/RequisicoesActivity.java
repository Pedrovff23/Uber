package com.example.app.uber.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationListenerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.uber.R;
import com.example.app.uber.adapter.RequisicoesAdapter;
import com.example.app.uber.databinding.ActivityRequisicoesBinding;
import com.example.app.uber.firebase.FireBase;
import com.example.app.uber.helper.RecyclerItemClickListener;
import com.example.app.uber.helper.UsuarioFirebase;
import com.example.app.uber.model.Requisicao;
import com.example.app.uber.model.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RequisicoesActivity extends AppCompatActivity {
    private final List<Requisicao> requisicaoList = new ArrayList<>();
    private ActivityRequisicoesBinding binding;
    private FirebaseAuth auth;
    private DatabaseReference database;
    private RecyclerView recyclerViewRequisicoes;
    private TextView textView;
    private ValueEventListener eventListener;
    private RequisicoesAdapter adapter;
    private Usuario motorista;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequisicoesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Configuração inicial
        recyclerViewRequisicoes = binding.recyclerRequisicoes;
        textView = binding.textResuldado;
        motorista = UsuarioFirebase.getDadosUsuarioLogado();

        auth = FireBase.getFirebaseAuth();
        database = FireBase.getFirebaseDatabase();

        getSupportActionBar().setTitle("Requisiçoes");

        //configurar adpter
        configurarAdapter();

        //Requisicoes
        recupearRequisicoes();

        //Recuperar localização motorista
        recuperarLocalizacaoUsuario();

    }

    private void configurarAdapter() {

        adapter = new RequisicoesAdapter(requisicaoList, getApplicationContext(), motorista);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerViewRequisicoes.setLayoutManager(layoutManager);
        recyclerViewRequisicoes.setHasFixedSize(true);
        recyclerViewRequisicoes.setAdapter(adapter);

    }

    private void adicionaeventoCliqueRecyclerView() {

        //Adicionar evento de click
        recyclerViewRequisicoes.addOnItemTouchListener(new RecyclerItemClickListener
                (getApplicationContext(), recyclerViewRequisicoes,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                Requisicao requisicao = requisicaoList.get(position);
                                abrirTelaCorrida(requisicao.getId(), motorista, false);
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {

                            }

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view,
                                                    int position, long id) {

                            }
                        }));
    }


    private void abrirTelaCorrida(String idRequisicao, Usuario motorista, boolean requisicaoAtiva) {

        Intent i = new Intent(RequisicoesActivity.this,
                CorridaActivity.class);
        i.putExtra("idRequisicao", idRequisicao);
        i.putExtra("motorista", motorista);
        i.putExtra("requisicaoAtiva", requisicaoAtiva);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    //recuperar localização do motorista
    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListenerCompat() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onLocationChanged(@NonNull Location location) {
                //recuperar latitude e longitude
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());

                //Atualizar no GeoFire
                UsuarioFirebase.atualizarDadosLocalizacao(
                        location.getLatitude(),
                        location.getLongitude());

                if (!latitude.isEmpty() && !longitude.isEmpty()) {
                    motorista.setLatitude(latitude);
                    motorista.setLongitude(longitude);

                    adicionaeventoCliqueRecyclerView();
                    locationManager.removeUpdates(locationListener);
                    adapter.notifyDataSetChanged();

                }
            }
        };

        //Solicitar atualização de localização
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    10,
                    locationListener);
        }

    }


    //Lista as requições com status "aguardando"
    private void recupearRequisicoes() {

        DatabaseReference requisicoes = database.child("requisicoes");

        Query requisicaoPesquisa = requisicoes.orderByChild("status")
                .equalTo(Requisicao.STATUS_AGUARDANDO);

        eventListener = requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 0) {

                    recyclerViewRequisicoes.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.GONE);

                    requisicaoList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Requisicao requisicaoAtual = ds.getValue(Requisicao.class);
                        requisicaoList.add(requisicaoAtual);
                    }
                    adapter.notifyDataSetChanged();

                } else {
                    textView.setVisibility(View.VISIBLE);
                    recyclerViewRequisicoes.setVisibility(View.GONE);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuSair) {

            auth.signOut();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStart() {
        super.onStart();
        verificaStatusRequisicao();
    }

    //Verifica se já tem uma viagem em andamento
    private void verificaStatusRequisicao() {

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference reference = FireBase.getFirebaseDatabase();

        DatabaseReference requisicoes = reference.child("requisicoes");
        Query requisioesPesquisa = requisicoes.orderByChild("motorista/id")
                .equalTo(usuarioLogado.getId());

        requisioesPesquisa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Requisicao requisicao = ds.getValue(Requisicao.class);
                    if (requisicao.getStatus().equals(Requisicao.STATUS_A_CAMINHO)
                            || requisicao.getStatus().equals(Requisicao.STATUS_VIAGEM)
                            || requisicao.getStatus().equals(Requisicao.STATUS_FINALIZADA)) {
                        motorista = requisicao.getMotorista();
                        abrirTelaCorrida(requisicao.getId(), motorista, true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    protected void onPause() {
        if (database != null && eventListener != null) {
            database.removeEventListener(eventListener);
        }
        super.onPause();
    }
}