class UserOptionLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_option_login)

        val studentButton = findViewById<Button>(R.id.btn_student_login)
        val instructorButton = findViewById<Button>(R.id.btn_instructor_login)

        studentButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra("user_type", "Student")
            startActivity(intent)
        }

        instructorButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra("user_type", "Instructor")
            startActivity(intent)
        }
    }
}
