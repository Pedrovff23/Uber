package com.example.app.uber.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.app.uber.activity.PassageiroActivity;
import com.example.app.uber.activity.RequisicoesActivity;
import com.example.app.uber.firebase.FireBase;
import com.example.app.uber.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UsuarioFirebase {

    private static FirebaseUser getUsuarioAtual() {

        return FireBase.getFirebaseAuth().getCurrentUser();
    }

    public static Usuario getDadosUsuarioLogado() {
        FirebaseUser user = getUsuarioAtual();
        Usuario usuario = new Usuario();
        usuario.setId(user.getUid());
        usuario.setEmail(user.getEmail());
        usuario.setNome(user.getDisplayName());
        return usuario;
    }

    public static void atualizarNomeUsuario(String nome) {

        try {

            FirebaseUser user = getUsuarioAtual();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(nome)
                    .build();
            user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()) {
                        Log.d("Perfil", "Erro ao atulizar perfil.");
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void redirecionaUsuarioLogado(Activity activity) {

        FirebaseUser user = getUsuarioAtual();
        if (user != null) {
            DatabaseReference usuariosRef = FireBase.getFirebaseDatabase()
                    .child("usuarios")
                    .child(getUidUsuario());

            usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    assert usuario != null;
                    String tipoUsuario = usuario.getTipo();
                    Intent i;
                    if (tipoUsuario.equals("M")) {
                        i = new Intent(activity, RequisicoesActivity.class);
                    } else {
                        i = new Intent(activity, PassageiroActivity.class);
                    }
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(i);
                    activity.finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }
    }

    public static String getUidUsuario() {
        return getUsuarioAtual().getUid();
    }

    public static void atualizarDadosLocalizacao(double lat, double lon){
        //Define nó de local de usuário
        DatabaseReference loclaUsuario = FireBase.getFirebaseDatabase().child("local_usuario");
        GeoFire geoFire = new GeoFire(loclaUsuario);

        //Recuperar dados do usuário logado
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        geoFire.setLocation(
                usuarioLogado.getId(),
                new GeoLocation(lat, lon),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if(error!=null){
                            Log.d("Erro","Erro ao salvar o local!");
                        }
                    }
                });
    }
}
