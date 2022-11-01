package com.example.app.uber.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app.uber.databinding.ActivityLoginBinding;
import com.example.app.uber.firebase.FireBase;
import com.example.app.uber.helper.UsuarioFirebase;
import com.example.app.uber.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private TextInputEditText email;
    private TextInputEditText senha;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        email = binding.editEmailLogin;
        senha = binding.editSenhaLogin;
        buttonLogin = binding.btnLogin;

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarEntradaDados();
            }
        });
    }

    public void validarEntradaDados() {

        String emailUsuario = Objects.requireNonNull(email.getText()).toString();
        String senhaUsuario = Objects.requireNonNull(senha.getText()).toString();

        if (!emailUsuario.isEmpty()) {
            if (!senhaUsuario.isEmpty()) {
                Usuario usuario = new Usuario();
                usuario.setEmail(emailUsuario);
                usuario.setSenha(senhaUsuario);
                validarLogin(usuario);
            } else {
                Toast.makeText(this, "Digite sua senha", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Digite seu email", Toast.LENGTH_SHORT).show();
        }
    }

    public void validarLogin(Usuario usuario) {

        FirebaseAuth auth = FireBase.getFirebaseAuth();
        auth.signInWithEmailAndPassword(usuario.getEmail(), usuario.getSenha())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            try {

                                //Verificar o tipo do usuário logado
                                UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);

                                Toast.makeText(LoginActivity.this, "Sucesso ao logar",
                                        Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {

                            String erroExcecao = "";

                            try {
                                throw Objects.requireNonNull(task.getException());

                            } catch (FirebaseAuthInvalidUserException e) {

                                erroExcecao = "Usuário não cadastrado";

                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                erroExcecao = "Credenciais inválida";

                            } catch (Exception e) {

                                erroExcecao = e.getMessage();
                                e.printStackTrace();
                            }

                            Toast.makeText(LoginActivity.this, "Erro: " + erroExcecao,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}