package com.alimrasid.prototype1.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.alimrasid.prototype1.ui.auth.login.LoginActivity
import com.alimrasid.prototype1.R
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var imgProfile: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        imgProfile = view.findViewById(R.id.imgProfile)

        val firebaseAuth = Firebase.auth
        val user: FirebaseUser? = firebaseAuth.currentUser

        if (user != null) {
            tvName.text = user.displayName
            tvEmail.text = user.email
            if (user.photoUrl != null) {
                Glide.with(requireContext())
                    .load(user.photoUrl)
                    .optionalCircleCrop()
                    .into(imgProfile)
            } else {
                Glide.with(requireContext())
                    .load(R.drawable.default_profile)
                    .optionalCircleCrop()
                    .into(imgProfile)
            }
        }

        val btnLogout: Button = view.findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            GoogleSignIn.getClient(requireContext(), gso).signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}
