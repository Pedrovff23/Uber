package com.example.app.uber.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app.uber.databinding.ActivityCadastroBinding;
import com.example.app.uber.firebase.FireBase;
import com.example.app.uber.helper.UsuarioFirebase;
import com.example.app.uber.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import java.util.Objects;

public class CadastroActivity extends AppCompatActivity {

    private ActivityCadastroBinding binding;
    private TextInputEditText nome;
    private TextInputEditText email;
    private TextInputEditText senha;
    private Button buttoncadastrar;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchCadastro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCadastroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        nome = binding.editCadastroNome;
        email = binding.editCadastroEmail;
        senha = binding.editCadastroSenha;
        buttoncadastrar = binding.btnCadastrar;
        switchCadastro = binding.switchCadastro;

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        buttoncadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarCadastro();
            }
        });
    }


    private void validarCadastro() {

        String nomeCad = nome.getText().toString();
        String emailCad = email.getText().toString();
        String senhaCad = senha.getText().toString();

        if (!nomeCad.isEmpty()) {
            if (!emailCad.isEmpty()) {
                if (!senhaCad.isEmpty()) {

                    Usuario usuario = new Usuario();
                    usuario.setNome(nomeCad);
                    usuario.setEmail(emailCad);
                    usuario.setSenha(senhaCad);
                    usuario.setTipo(verificaTipoUsuario());

                    cadastarFirebase(usuario);

                } else {
                    Toast.makeText(this, "Preencha sua senha", Toast.LENGTH_SHORT)
                            .show();
                }
            } else {
                Toast.makeText(this, "Preencha o seu email", Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            Toast.makeText(this, "Preencha o seu nome completo", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public String verificaTipoUsuario() {
        return switchCadastro.isChecked() ? "M" : "P";
    }

    private void cadastarFirebase(Usuario usuario) {


        FirebaseAuth auth = FireBase.getFirebaseAuth();


        auth.createUserWithEmailAndPassword(usuario.getEmail(), usuario.getSenha())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            try {

                                String uidUsuario = task.getResult().getUser().getUid();

                                usuario.setId(uidUsuario);
                                usuario.salvar();

                                //Atualizar nome no UserProfile
                                UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                                if (Objects.equals(usuario.getTipo(), "P")) {

                                    String mensagem = "Sucesso ao cadastrar passageiro";

                                    startActivity(new Intent(CadastroActivity.this,
                                            PassageiroActivity.class));

                                    finish();

                                    Toast.makeText(CadastroActivity.this, mensagem,
                                            Toast.LENGTH_SHORT).show();
                                } else {

                                    String mensagem = "Sucesso ao cadastrar motorista";

                                    startActivity(new Intent(CadastroActivity.this,
                                            RequisicoesActivity.class));

                                    finish();

                                    Toast.makeText(CadastroActivity.this, mensagem,
                                            Toast.LENGTH_SHORT).show();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            String error = "";
                            try {

                                throw Objects.requireNonNull(task.getException());

                            } catch (FirebaseAuthWeakPasswordException e) {
                                error = "Digite uma senha mais forte!";

                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                error = "Digite um E-mail válido!";

                            } catch (FirebaseAuthUserCollisionException e) {
                                error = "Conta já cadastrada!";

                            } catch (Exception e) {
                                error = "Erro ao cadastrar usuario: " + e.getMessage();
                                e.printStackTrace();
                            }
                            Toast.makeText(CadastroActivity.this, "Erro: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
