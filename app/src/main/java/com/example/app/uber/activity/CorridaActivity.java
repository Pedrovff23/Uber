package com.example.app.uber.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationListenerCompat;

import com.example.app.uber.R;
import com.example.app.uber.databinding.ActivityCorridaBinding;
import com.example.app.uber.firebase.FireBase;
import com.example.app.uber.helper.Local;
import com.example.app.uber.helper.UsuarioFirebase;
import com.example.app.uber.model.Destino;
import com.example.app.uber.model.Requisicao;
import com.example.app.uber.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.Objects;

public class CorridaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityCorridaBinding binding;
    private Button aceitarCorrida;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private DatabaseReference requisicoesRef;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private ValueEventListener valueEventListener;
    private String status;
    private Boolean requisicaoAtiva;
    private FloatingActionButton fabRota;
    private Destino destino;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCorridaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        mapFragment.getMapAsync(this);

        //Configuração inicial
        aceitarCorrida = binding.buttonAceitarCorrida;
        fabRota = binding.fabRota;
        firebaseRef = FireBase.getFirebaseDatabase();
        requisicao = new Requisicao();

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Iniciar Corrida");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //Recupear Dados
        if (getIntent().getExtras().containsKey("idRequisicao") &&
                getIntent().getExtras().containsKey("motorista")) {

            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLongitude()),
                    Double.parseDouble(motorista.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            requisicao.setId(idRequisicao);
        }

        aceitarCorrida.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                //Configurar Requisicão
                requisicao.setId(idRequisicao);
                requisicao.setMotorista(motorista);
                requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);
                requisicao.atualizar();
            }
        });

        fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status != null && !status.isEmpty()) {
                    String lat = "";
                    String lon = "";

                    switch (status) {
                        case Requisicao.STATUS_A_CAMINHO:
                            lat = String.valueOf(localPassageiro.latitude);
                            lon = String.valueOf(localPassageiro.longitude);
                            break;
                        case Requisicao.STATUS_VIAGEM:
                            lat = destino.getLatitude();
                            lon = destino.getLongitude();
                            break;
                    }
                    //Abrir Rota
                    String latLong = lat + "," + lon;
                    Uri uri = Uri.parse("google.navigation:q=" + latLong + "&mode=d");
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    i.setPackage("com.google.android.apps.maps");
                    startActivity(i);

                }

            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.mMap = googleMap;

        //recuperar localização do usuario
        recuperarLocalizacaoMotorista();

    }

    private void recuperarLocalizacaoMotorista() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListenerCompat() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localMotorista = new LatLng(latitude, longitude);

                //Atualizar no GeoFire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //Atualizar localização no FireBase
                motorista.setLatitude(String.valueOf(latitude));
                motorista.setLongitude(String.valueOf(longitude));
                requisicao.setMotorista(motorista);
                requisicao.atualizarLocalizacaoMotorista();

                //verifica status da requisição
                verificaStatusRequisicao();
            }
        };

        //Solicitar atualização de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener);
        }

    }

    private void verificaStatusRequisicao() {

        requisicoesRef = firebaseRef.child("requisicoes").child(idRequisicao);

        valueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Requisicao requisicao = snapshot.getValue(Requisicao.class);
                if (requisicao != null) {
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude()));
                    status = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alterarInterfaceStatusRequisicao(status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        requisicoesRef.addValueEventListener(valueEventListener);
    }

    private void alterarInterfaceStatusRequisicao(String status) {

        switch (status) {
            case Requisicao.STATUS_AGUARDANDO:
                requisicaoAguardando();
                //Exibe marcador do motorista
                adicionarMarcadorMotorista(localMotorista, motorista.getNome());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localMotorista, 20));
                break;
            case Requisicao.STATUS_A_CAMINHO:
                requisicaoAcaminho();
                break;
            case Requisicao.STATUS_VIAGEM:
                requisicaoViagem();
                break;
            case Requisicao.STATUS_FINALIZADA:
                requisicaoFinalizada();
                requisicaoAtiva = false;
                break;
            case Requisicao.STATUS_CANCELADA:
                requisicaoCancelada();
                break;
        }
    }

    private void requisicaoCancelada(){
        Toast.makeText(this, "Requisição foi cancelada pelo passageiro",
                Toast.LENGTH_SHORT).show();
        startActivity(new Intent(CorridaActivity.this,RequisicoesActivity.class));
    }

    @SuppressLint("SetTextI18n")
    private void requisicaoFinalizada() {

        fabRota.setVisibility(View.GONE);
        aceitarCorrida.setEnabled(false);

        if (marcadorMotorista != null) {
            marcadorMotorista.remove();
        }

        //Exibir marcador destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        //calcular distancia
        float distancia = Local.calcularDistancia(localPassageiro,localDestino);
        float valor = distancia * 8;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String resultado = decimalFormat.format(valor);
        aceitarCorrida.setText("Corrida finalizada - R$ " + resultado);
    }

    private void centralizarMarcador(LatLng local) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 20));
    }

    @SuppressLint("SetTextI18n")
    private void requisicaoAguardando() {
        aceitarCorrida.setText("Aceitar Corrida");
        centralizarMarcador(localMotorista);
    }

    @SuppressLint("SetTextI18n")
    private void requisicaoAcaminho() {
        aceitarCorrida.setText("A caminho do passageiro");
        fabRota.setVisibility(View.VISIBLE);

        //Exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //Exibe o marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Centralizar dois marcadores;
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //Iniciar monitoramento do motorista/passageiro
        iniciarMonitoramento(motorista, localPassageiro, Requisicao.STATUS_VIAGEM);

    }

    @SuppressLint("SetTextI18n")
    private void requisicaoViagem() {

        //Alterar interface
        fabRota.setVisibility(View.VISIBLE);
        aceitarCorrida.setText("A caminho do destino");

        //Exibir marcador do motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //Exibir marcador de destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude()));
        adicionarMarcadorDestino(localDestino, "Destino");

        //Centralizar marcadores motorista / destino
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

        //Iniciar monitoramento do motorista/passageiro
        iniciarMonitoramento(motorista, localDestino, Requisicao.STATUS_FINALIZADA);
    }


    private void iniciarMonitoramento(Usuario origem, LatLng localDestino, String status) {
        //Inicializar GeoFire
        DatabaseReference localUsuario = FireBase.getFirebaseDatabase().child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //Adicionar círculo no passageiro
        Circle circle = mMap.addCircle(
                new CircleOptions()
                        .center(localDestino)
                        .radius(50)// em metros
                        .fillColor(Color.argb(90, 255, 153, 0))
                        .strokeColor(Color.argb(190, 255, 153, 0))
        );

        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localDestino.latitude, localDestino.longitude),
                0.05//em Km
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (key.equals(origem.getId())) {
                    //Alterar o status da requisição
                    requisicao.setId(idRequisicao);
                    requisicao.setStatus(status);
                    requisicao.atualizarStatus();

                    //Remove circule and GeoQuery
                    geoQuery.removeAllListeners();
                    circle.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void adicionarMarcadorMotorista(LatLng localizacaoMot, String titulo) {

        if (marcadorMotorista != null) {
            marcadorMotorista.remove();
        }

        marcadorMotorista = mMap.addMarker(new MarkerOptions()
                .position(localizacaoMot)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro)));
    }

    private void adicionarMarcadorPassageiro(LatLng localizacaoPas, String titulo) {

        if (marcadorPassageiro != null) {
            marcadorPassageiro.remove();
        }
        marcadorPassageiro = mMap.addMarker(new MarkerOptions()
                .position(localizacaoPas)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario)));
    }

    private void adicionarMarcadorDestino(LatLng localizacaoMot, String titulo) {

        if (marcadorPassageiro != null) {
            marcadorPassageiro.remove();
        }
        if (marcadorDestino != null) {
            marcadorDestino.remove();
        }
        marcadorDestino = mMap.addMarker(new MarkerOptions()
                .position(localizacaoMot)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino)));
    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.40);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno));
    }


    @Override
    public boolean onSupportNavigateUp() {
        if (requisicaoAtiva) {
            Toast.makeText(this,
                    "Necessário encerrar a requisição atual!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Intent i = new Intent(CorridaActivity.this, RequisicoesActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
        //Verificar o status da requisição para encerrar
        if(status!=null&&!status.isEmpty()){
            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
            requisicao.atualizarStatus();
        }
        return false;
    }

    @Override
    protected void onStop() {
        if (firebaseRef != null) {
            firebaseRef.removeEventListener(valueEventListener);
        }
        super.onStop();
    }
}