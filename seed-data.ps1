$ErrorActionPreference = "Continue"

# ============================================================================
#  SEED DATA INSERTION SCRIPT
#  Inserts all master data, users, mappings for the Attendance System
#  All passwords: "password"
#  All emails: jangamswamy306+{username}@gmail.com
# ============================================================================

$BASE = "http://localhost:8080"
$PASSWORD = "password"
$EMAIL_PREFIX = "jangamswamy306"

function apiCall($method, $path, $jsonBody=$null, $token=$null) {
    $uri = "$BASE$path"
    $headers = @{ "Content-Type" = "application/json" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    try {
        $params = @{ Uri=$uri; Method=$method; Headers=$headers; UseBasicParsing=$true }
        if ($jsonBody) { $params["Body"] = $jsonBody }
        $resp = Invoke-WebRequest @params
        return @{ status=[int]$resp.StatusCode; body=($resp.Content | ConvertFrom-Json); raw=$resp.Content }
    } catch {
        $sc = 0; $rb = ""
        if ($_.Exception.Response) {
            $sc = [int]$_.Exception.Response.StatusCode
            try {
                $sr = $_.Exception.Response.GetResponseStream()
                $rd = New-Object System.IO.StreamReader($sr)
                $rd.BaseStream.Position = 0
                $rb = $rd.ReadToEnd(); $rd.Close()
            } catch { $rb = '{"message":"Could not read"}' }
        }
        try { $p = $rb | ConvertFrom-Json } catch { $p = @{ message=$rb } }
        return @{ status=$sc; body=$p; raw=$rb }
    }
}

function sql($query) {
    $mysql = "mysql"
    $args = @(
        "-h", "viaduct.proxy.rlwy.net",
        "-P", "39532",
        "-u", "root",
        "-pwMnNsEmfWtWyrpSpLMzgWGeWSPqBbvMd",
        "-D", "railway",
        "-N",
        "-B",
        "-e", $query
    )
    try {
        $out = & $mysql @args 2>$null
        return $out
    } catch {
        return $null
    }
}


function logStep($m) { Write-Host "`n>> $m" -ForegroundColor Cyan }
function logOK($m)   { Write-Host "   [OK]   $m" -ForegroundColor Green }
function logFAIL($m) { Write-Host "   [FAIL] $m" -ForegroundColor Red }
function logINFO($m) { Write-Host "   [INFO] $m" -ForegroundColor Yellow }

# ============================================================================
# STEP 1: INSERT SUBJECTS (19)
# ============================================================================
logStep "Inserting 19 subjects ..."
$subjectSQL = @"
INSERT INTO subjects (name) VALUES
('Introduction to Programming'),
('Java Programming'),
('Advanced Data Structures and Algorithms'),
('Operating Systems'),
('Database Management Systems'),
('Digital Electronics'),
('Signals and Systems'),
('VLSI Design'),
('Microprocessors and Microcontrollers'),
('Communication Systems'),
('Structural Analysis'),
('Fluid Mechanics'),
('Surveying'),
('Concrete Technology'),
('Geotechnical Engineering'),
('Mathematics I'),
('Chemistry'),
('Physics'),
('English Communication');
"@
sql $subjectSQL
$sc = (sql "SELECT COUNT(*) FROM subjects;")
logOK "Subjects: $sc rows"

# ============================================================================
# STEP 2: INSERT SECTIONS (9)
# ============================================================================
logStep "Inserting 9 sections ..."
$sectionSQL = @"
INSERT INTO sections (name, department_name) VALUES
('CSE-A','CSE'),('CSE-B','CSE'),('CSE-C','CSE'),
('ECE-A','ECE'),('ECE-B','ECE'),('ECE-C','ECE'),
('CIVIL-A','Civil'),('CIVIL-B','Civil'),('CIVIL-C','Civil');
"@
sql $sectionSQL
$sc = (sql "SELECT COUNT(*) FROM sections;")
logOK "Sections: $sc rows"

# ============================================================================
# STEP 3: INSERT ROOMS (25)
# ============================================================================
logStep "Inserting 25 rooms ..."
$roomSQL = @"
INSERT INTO rooms (room_number, beacon_uuid, width, length, safe_radius_meters) VALUES
('001','f7826da6-4fa2-4e98-8024-bc5b71e0893e',8.5,10.2,15.0),
('002','5a4bcfce-174e-4bac-a814-092e77f6b7e5',8.5,10.2,15.0),
('003','b9407f30-f5f8-466e-aff9-25556b57fe6d',9.0,11.0,16.0),
('004','e2c56db5-dffb-48d2-b060-d0f5a71096e0',9.0,11.0,16.0),
('005','7d4c3b8f-9e2a-4c6d-8f1b-3a5e7d9c2b1a',10.0,12.0,18.0),
('006','1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',10.0,12.0,18.0),
('007','9f8e7d6c-5b4a-3918-2716-1a0b9c8d7e6f',8.0,9.5,14.0),
('008','a1b2c3d4-e5f6-4789-a0bc-def123456789',8.0,9.5,14.0),
('009','f1e2d3c4-b5a6-4798-8069-5a4b3c2d1e0f',9.5,11.5,17.0),
('010','6b7c8d9e-0f1a-4b2c-3d4e-5f6a7b8c9d0e',9.5,11.5,17.0),
('011','2c3d4e5f-6a7b-4c8d-9e0f-1a2b3c4d5e6f',8.5,10.0,15.5),
('012','3d4e5f6a-7b8c-4d9e-0f1a-2b3c4d5e6f7a',8.5,10.0,15.5),
('013','4e5f6a7b-8c9d-4e0f-1a2b-3c4d5e6f7a8b',10.5,12.5,19.0),
('014','5f6a7b8c-9d0e-4f1a-2b3c-4d5e6f7a8b9c',10.5,12.5,19.0),
('015','6a7b8c9d-0e1f-4a2b-3c4d-5e6f7a8b9c0d',9.0,10.5,16.5),
('016','7b8c9d0e-1f2a-4b3c-4d5e-6f7a8b9c0d1e',9.0,10.5,16.5),
('017','8c9d0e1f-2a3b-4c4d-5e6f-7a8b9c0d1e2f',8.0,9.0,14.5),
('018','9d0e1f2a-3b4c-4d5e-6f7a-8b9c0d1e2f3a',8.0,9.0,14.5),
('019','0e1f2a3b-4c5d-4e6f-7a8b-9c0d1e2f3a4b',11.0,13.0,20.0),
('020','1f2a3b4c-5d6e-4f7a-8b9c-0d1e2f3a4b5c',11.0,13.0,20.0),
('021','2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d',9.5,10.8,17.5),
('022','3b4c5d6e-7f8a-4b9c-0d1e-2f3a4b5c6d7e',9.5,10.8,17.5),
('023','4c5d6e7f-8a9b-4c0d-1e2f-3a4b5c6d7e8f',10.0,11.5,18.5),
('024','5d6e7f8a-9b0c-4d1e-2f3a-4b5c6d7e8f9a',10.0,11.5,18.5),
('025','6e7f8a9b-0c1d-4e2f-3a4b-5c6d7e8f9a0b',12.0,14.0,22.0);
"@
sql $roomSQL
$sc = (sql "SELECT COUNT(*) FROM rooms;")
logOK "Rooms: $sc rows"

# ============================================================================
# STEP 4: LOGIN AS ADMIN
# ============================================================================
logStep "Logging in as admin (Admin@123) ..."
$r = apiCall "POST" "/auth/login" '{"username":"admin","password":"Admin@123"}'
if ($r.status -ne 200) {
    logFAIL "Admin login failed: $($r.body.message)"
    exit 1
}
$adminToken = $r.body.accessToken
logOK "Admin logged in"

# ============================================================================
# STEP 5: CREATE ALL TEACHERS (as SUBJECT_TEACHER for subject mappings)
# ============================================================================
logStep "Creating 35 teachers via admin API ..."

# Format: username, display name, actual role, subject IDs, section IDs
$teachers = @(
    # --- CSE Department (10) ---
    @{u="TC001CSE45"; n="Priya Sharma";           r="SUBJECT_TEACHER"; subs=@(1,16);  secs=@(1)},
    @{u="TC002CSE89"; n="Rajesh Kumar";            r="SUBJECT_TEACHER"; subs=@(2);     secs=@(1)},
    @{u="TC003CSE12"; n="Anita Reddy";             r="CLASS_TEACHER";   subs=@(3);     secs=@(1)},
    @{u="TC004CSE67"; n="Vikram Singh";            r="SUBJECT_TEACHER"; subs=@(4,5);   secs=@(2)},
    @{u="TC005CSE34"; n="Deepa Nair";              r="CLASS_TEACHER";   subs=@(1);     secs=@(2)},
    @{u="TC006CSE90"; n="Suresh Patel";            r="SUBJECT_TEACHER"; subs=@(2);     secs=@(2)},
    @{u="TC007CSE23"; n="Meera Iyer";              r="CLASS_TEACHER";   subs=@(3);     secs=@(3)},
    @{u="TC008CSE56"; n="Arun Verma";              r="SUBJECT_TEACHER"; subs=@(5);     secs=@(3)},
    @{u="TC009CSE78"; n="Kavita Joshi";            r="SUBJECT_TEACHER"; subs=@(4);     secs=@(3)},
    @{u="TC010CSE01"; n="Ramesh Das";              r="HOD";             subs=@(1,5);   secs=@(1,2,3)},
    # --- ECE Department (10) ---
    @{u="TE001ECE45"; n="Sanjay Gupta";            r="SUBJECT_TEACHER"; subs=@(6);     secs=@(4)},
    @{u="TE002ECE89"; n="Lakshmi Rao";             r="CLASS_TEACHER";   subs=@(7);     secs=@(4)},
    @{u="TE003ECE12"; n="Mohan Krishna";           r="SUBJECT_TEACHER"; subs=@(8);     secs=@(4)},
    @{u="TE004ECE67"; n="Pooja Malhotra";          r="CLASS_TEACHER";   subs=@(9);     secs=@(5)},
    @{u="TE005ECE34"; n="Karthik Reddy";           r="SUBJECT_TEACHER"; subs=@(10,6);  secs=@(5)},
    @{u="TE006ECE90"; n="Sneha Desai";             r="CLASS_TEACHER";   subs=@(7);     secs=@(5)},
    @{u="TE007ECE23"; n="Naveen Sharma";           r="CLASS_TEACHER";   subs=@(8);     secs=@(6)},
    @{u="TE008ECE56"; n="Divya Pillai";            r="SUBJECT_TEACHER"; subs=@(9);     secs=@(6)},
    @{u="TE009ECE78"; n="Amit Bhatt";              r="SUBJECT_TEACHER"; subs=@(10);    secs=@(6)},
    @{u="TE010ECE01"; n="Radha Menon";             r="HOD";             subs=@(6,18);  secs=@(4,5,6)},
    # --- CIVIL Department (10) ---
    @{u="TV001CIV45"; n="Venkat Subramanian";      r="SUBJECT_TEACHER"; subs=@(11);    secs=@(7)},
    @{u="TV002CIV89"; n="Shobha Ramesh";           r="CLASS_TEACHER";   subs=@(12);    secs=@(7)},
    @{u="TV003CIV12"; n="Prakash Nambiar";         r="SUBJECT_TEACHER"; subs=@(13);    secs=@(7)},
    @{u="TV004CIV67"; n="Sunita Kaur";             r="CLASS_TEACHER";   subs=@(14);    secs=@(8)},
    @{u="TV005CIV34"; n="Ganesh Hegde";            r="SUBJECT_TEACHER"; subs=@(15,11); secs=@(8)},
    @{u="TV006CIV90"; n="Nandini Rao";             r="CLASS_TEACHER";   subs=@(12);    secs=@(8)},
    @{u="TV007CIV23"; n="Ashok Jain";              r="CLASS_TEACHER";   subs=@(13);    secs=@(9)},
    @{u="TV008CIV56"; n="Vandana Mishra";          r="SUBJECT_TEACHER"; subs=@(14);    secs=@(9)},
    @{u="TV009CIV78"; n="Dinesh Kulkarni";         r="SUBJECT_TEACHER"; subs=@(15);    secs=@(9)},
    @{u="TV010CIV01"; n="Aruna Srinivas";          r="HOD";             subs=@(11,17); secs=@(7,8,9)},
    # --- H&BS Department (5) - teach ALL sections ---
    @{u="TH001HBS45"; n="Madhuri Chandra";         r="SUBJECT_TEACHER"; subs=@(16);    secs=@(1,2,3,4,5,6,7,8,9)},
    @{u="TH002HBS89"; n="Subhash Yadav";           r="SUBJECT_TEACHER"; subs=@(17);    secs=@(1,2,3,4,5,6,7,8,9)},
    @{u="TH003HBS12"; n="Ramya Bhat";              r="SUBJECT_TEACHER"; subs=@(18);    secs=@(1,2,3,4,5,6,7,8,9)},
    @{u="TH004HBS67"; n="Krishnan Pillai";         r="SUBJECT_TEACHER"; subs=@(19);    secs=@(1,2,3,4,5,6,7,8,9)},
    @{u="TH005HBS34"; n="Swapna Das";              r="HOD";             subs=@(16,17); secs=@(1,2,3,4,5,6,7,8,9)}
)

$tCreated = 0
foreach ($t in $teachers) {
    $email = "$EMAIL_PREFIX+$($t.u)@gmail.com"
    $body = @{
        username   = $t.u
        email      = $email
        password   = $PASSWORD
        role       = "SUBJECT_TEACHER"
        subjectIds = $t.subs
    } | ConvertTo-Json -Compress
    $r = apiCall "POST" "/admin/users" $body $adminToken
    if ($r.status -eq 200) {
        $tCreated++
        Write-Host "   [$tCreated/35] $($t.u) - $($t.n)" -ForegroundColor Green
    } else {
        $msg = $null
        try { $msg = $r.body.message } catch { $msg = $r.raw }
        logFAIL "$($t.u): status=$($r.status) | $msg"
    }
}
logOK "Teachers created: $tCreated / 35"

# ============================================================================
# STEP 6: FIX ROLES FOR HODs AND CLASS_TEACHERs
# ============================================================================
logStep "Updating roles for HODs and CLASS_TEACHERs ..."
sql "UPDATE users SET role='HOD' WHERE username IN ('TC010CSE01','TE010ECE01','TV010CIV01','TH005HBS34');"
sql "UPDATE users SET role='CLASS_TEACHER' WHERE username IN ('TC003CSE12','TC005CSE34','TC007CSE23','TE002ECE89','TE004ECE67','TE006ECE90','TE007ECE23','TV002CIV89','TV004CIV67','TV006CIV90','TV007CIV23');"
$hodCount = (sql "SELECT COUNT(*) FROM users WHERE role='HOD';")
$ctCount = (sql "SELECT COUNT(*) FROM users WHERE role='CLASS_TEACHER';")
$stCount = (sql "SELECT COUNT(*) FROM users WHERE role='SUBJECT_TEACHER';")
logOK "HODs: $hodCount | CLASS_TEACHERs: $ctCount | SUBJECT_TEACHERs: $stCount"

# ============================================================================
# STEP 7: CREATE ALL 90 STUDENTS
# ============================================================================
logStep "Creating 90 students via admin API ..."

$students = @(
    # CSE-A (Section 1)
    @{u="22A91A0501"; n="Aarav Sharma";    sec=1},
    @{u="22A91A0502"; n="Ananya Gupta";    sec=1},
    @{u="22A91A0503"; n="Arjun Reddy";     sec=1},
    @{u="22A91A0504"; n="Diya Patel";      sec=1},
    @{u="22A91A0505"; n="Ishaan Kumar";    sec=1},
    @{u="22A91A0506"; n="Kavya Nair";      sec=1},
    @{u="22A91A0507"; n="Rohan Singh";     sec=1},
    @{u="22A91A0508"; n="Sanya Verma";     sec=1},
    @{u="22A91A0509"; n="Vivaan Joshi";    sec=1},
    @{u="22A91A0510"; n="Zara Iyer";       sec=1},
    # CSE-B (Section 2)
    @{u="22A91A0511"; n="Aditya Malhotra"; sec=2},
    @{u="22A91A0512"; n="Aisha Das";       sec=2},
    @{u="22A91A0513"; n="Dhruv Bhatt";     sec=2},
    @{u="22A91A0514"; n="Isha Desai";      sec=2},
    @{u="22A91A0515"; n="Karan Pillai";    sec=2},
    @{u="22A91A0516"; n="Myra Menon";      sec=2},
    @{u="22A91A0517"; n="Pranav Rao";      sec=2},
    @{u="22A91A0518"; n="Riya Hegde";      sec=2},
    @{u="22A91A0519"; n="Shreyas Srinivas";sec=2},
    @{u="22A91A0520"; n="Tanvi Jain";      sec=2},
    # CSE-C (Section 3)
    @{u="22A91A0521"; n="Atharv Kulkarni"; sec=3},
    @{u="22A91A0522"; n="Devika Mishra";   sec=3},
    @{u="22A91A0523"; n="Harsh Chandra";   sec=3},
    @{u="22A91A0524"; n="Jiya Yadav";      sec=3},
    @{u="22A91A0525"; n="Krish Bhat";      sec=3},
    @{u="22A91A0526"; n="Navya Nambiar";   sec=3},
    @{u="22A91A0527"; n="Ritvik Kaur";     sec=3},
    @{u="22A91A0528"; n="Saanvi Subramanian"; sec=3},
    @{u="22A91A0529"; n="Vihaan Ramesh";   sec=3},
    @{u="22A91A0530"; n="Yash Krishna";    sec=3},
    # ECE-A (Section 4)
    @{u="22A91B0501"; n="Aarushi Agarwal"; sec=4},
    @{u="22A91B0502"; n="Advait Kapoor";   sec=4},
    @{u="22A91B0503"; n="Avni Saxena";     sec=4},
    @{u="22A91B0504"; n="Darsh Bansal";    sec=4},
    @{u="22A91B0505"; n="Kiara Mehta";     sec=4},
    @{u="22A91B0506"; n="Laksh Tripathi";  sec=4},
    @{u="22A91B0507"; n="Mira Choudhury";  sec=4},
    @{u="22A91B0508"; n="Nirav Singh";     sec=4},
    @{u="22A91B0509"; n="Pari Chatterjee"; sec=4},
    @{u="22A91B0510"; n="Reyansh Ghosh";   sec=4},
    # ECE-B (Section 5)
    @{u="22A91B0511"; n="Anika Dixit";     sec=5},
    @{u="22A91B0512"; n="Arnav Sen";       sec=5},
    @{u="22A91B0513"; n="Charvi Bhardwaj"; sec=5},
    @{u="22A91B0514"; n="Eshan Pandey";    sec=5},
    @{u="22A91B0515"; n="Kavish Tiwari";   sec=5},
    @{u="22A91B0516"; n="Mahira Dubey";    sec=5},
    @{u="22A91B0517"; n="Neil Jha";        sec=5},
    @{u="22A91B0518"; n="Shanaya Sinha";   sec=5},
    @{u="22A91B0519"; n="Veer Pathak";     sec=5},
    @{u="22A91B0520"; n="Zaara Chauhan";   sec=5},
    # ECE-C (Section 6)
    @{u="22A91B0521"; n="Aadhya Thakur";   sec=6},
    @{u="22A91B0522"; n="Abhi Varma";      sec=6},
    @{u="22A91B0523"; n="Divyansh Soni";   sec=6},
    @{u="22A91B0524"; n="Ishita Rawat";    sec=6},
    @{u="22A91B0525"; n="Kabir Rana";      sec=6},
    @{u="22A91B0526"; n="Naina Saxena";    sec=6},
    @{u="22A91B0527"; n="Rudra Bhatt";     sec=6},
    @{u="22A91B0528"; n="Sia Malhotra";    sec=6},
    @{u="22A91B0529"; n="Vedant Kapoor";   sec=6},
    @{u="22A91B0530"; n="Yana Deshmukh";   sec=6},
    # CIVIL-A (Section 7)
    @{u="22A91C0501"; n="Aarav Kumar";     sec=7},
    @{u="22A91C0502"; n="Aditi Shah";      sec=7},
    @{u="22A91C0503"; n="Ayaan Patel";     sec=7},
    @{u="22A91C0504"; n="Disha Joshi";     sec=7},
    @{u="22A91C0505"; n="Kartik Singh";    sec=7},
    @{u="22A91C0506"; n="Manvi Gupta";     sec=7},
    @{u="22A91C0507"; n="Ranbir Nair";     sec=7},
    @{u="22A91C0508"; n="Suhana Reddy";    sec=7},
    @{u="22A91C0509"; n="Varun Verma";     sec=7},
    @{u="22A91C0510"; n="Zoya Iyer";       sec=7},
    # CIVIL-B (Section 8)
    @{u="22A91C0511"; n="Alok Das";        sec=8},
    @{u="22A91C0512"; n="Deepika Bhatt";   sec=8},
    @{u="22A91C0513"; n="Gautam Desai";    sec=8},
    @{u="22A91C0514"; n="Jasmine Pillai";  sec=8},
    @{u="22A91C0515"; n="Kian Menon";      sec=8},
    @{u="22A91C0516"; n="Meera Rao";       sec=8},
    @{u="22A91C0517"; n="Omkar Hegde";     sec=8},
    @{u="22A91C0518"; n="Rhea Srinivas";   sec=8},
    @{u="22A91C0519"; n="Siddharth Jain";  sec=8},
    @{u="22A91C0520"; n="Tara Kulkarni";   sec=8},
    # CIVIL-C (Section 9)
    @{u="22A91C0521"; n="Aaryan Mishra";   sec=9},
    @{u="22A91C0522"; n="Anvi Chandra";    sec=9},
    @{u="22A91C0523"; n="Dhanush Yadav";   sec=9},
    @{u="22A91C0524"; n="Kiara Bhat";      sec=9},
    @{u="22A91C0525"; n="Krish Nambiar";   sec=9},
    @{u="22A91C0526"; n="Nidhi Kaur";      sec=9},
    @{u="22A91C0527"; n="Ritesh Subramanian"; sec=9},
    @{u="22A91C0528"; n="Sara Ramesh";     sec=9},
    @{u="22A91C0529"; n="Viraj Krishna";   sec=9},
    @{u="22A91C0530"; n="Yamini Agarwal";  sec=9}
)

$sCreated = 0
foreach ($s in $students) {
    $email = "$EMAIL_PREFIX+$($s.u)@gmail.com"
    $body = @{
        username  = $s.u
        email     = $email
        password  = $PASSWORD
        role      = "STUDENT"
        sectionId = $s.sec
    } | ConvertTo-Json -Compress
    $r = apiCall "POST" "/admin/users" $body $adminToken
    if ($r.status -eq 200) {
        $sCreated++
        if ($sCreated % 10 -eq 0) {
            Write-Host "   [$sCreated/90] ... $($s.u) - $($s.n)" -ForegroundColor Green
        }
    } else {
        $msg = $null
        try { $msg = $r.body.message } catch { $msg = $r.raw }
        logFAIL "$($s.u): status=$($r.status) | $msg"
    }
}
logOK "Students created: $sCreated / 90"

# ============================================================================
# STEP 8: UPDATE TEACHER NAMES
# ============================================================================
logStep "Updating teacher display names ..."
foreach ($t in $teachers) {
    $safeName = $t.n -replace "'", "''"
    sql "UPDATE teachers SET name='$safeName' WHERE teacher_id='$($t.u)';"
}
logOK "35 teacher names updated"

# ============================================================================
# STEP 9: UPDATE STUDENT NAMES
# ============================================================================
logStep "Updating student display names ..."
foreach ($s in $students) {
    $safeName = $s.n -replace "'", "''"
    sql "UPDATE students SET name='$safeName' WHERE student_id='$($s.u)';"
}
logOK "90 student names updated"

# ============================================================================
# STEP 10: UPDATE ADMIN EMAIL & PASSWORD
# ============================================================================
logStep "Updating admin email and password ..."
sql "UPDATE users u1, users u2 SET u1.password = u2.password, u1.email = '${EMAIL_PREFIX}+admin@gmail.com' WHERE u1.username = 'admin' AND u2.username = 'TC001CSE45';"
logOK "Admin email -> ${EMAIL_PREFIX}+admin@gmail.com | password -> 'password'"

# ============================================================================
# STEP 11: INSERT TEACHER-SECTION MAPPINGS
# ============================================================================
logStep "Inserting teacher-section mappings ..."
$tsMapped = 0
foreach ($t in $teachers) {
    foreach ($sec in $t.secs) {
        sql "INSERT IGNORE INTO teacher_sections (teacher_id, section_id) SELECT t.id, $sec FROM teachers t WHERE t.teacher_id='$($t.u)';"
        $tsMapped++
    }
}
$tsCount = (sql "SELECT COUNT(*) FROM teacher_sections;")
logOK "Teacher-section mappings: $tsCount rows"

# ============================================================================
# STEP 12: INSERT SUBJECT-SECTION MAPPINGS
# ============================================================================
logStep "Inserting subject-section mappings ..."

# CSE subjects (1-5) -> CSE sections (1-3)
# ECE subjects (6-10) -> ECE sections (4-6)
# CIVIL subjects (11-15) -> CIVIL sections (7-9)
# HBS subjects (16-19) -> ALL sections (1-9)

$ssMappings = @()
# CSE
for ($sub = 1; $sub -le 5; $sub++) {
    for ($sec = 1; $sec -le 3; $sec++) { $ssMappings += ,@($sub, $sec) }
}
# ECE
for ($sub = 6; $sub -le 10; $sub++) {
    for ($sec = 4; $sec -le 6; $sec++) { $ssMappings += ,@($sub, $sec) }
}
# CIVIL
for ($sub = 11; $sub -le 15; $sub++) {
    for ($sec = 7; $sec -le 9; $sec++) { $ssMappings += ,@($sub, $sec) }
}
# HBS -> ALL
for ($sub = 16; $sub -le 19; $sub++) {
    for ($sec = 1; $sec -le 9; $sec++) { $ssMappings += ,@($sub, $sec) }
}

# Build single INSERT
$ssValues = ($ssMappings | ForEach-Object { "($($_[0]),$($_[1]))" }) -join ","
sql "INSERT IGNORE INTO subject_sections (subject_id, section_id) VALUES $ssValues;"
$ssCount = (sql "SELECT COUNT(*) FROM subject_sections;")
logOK "Subject-section mappings: $ssCount rows"

# ============================================================================
# VERIFICATION
# ============================================================================
logStep "Verifying final state ..."
$counts = @(
    @{t="users";     e=126},
    @{t="teachers";  e=35},
    @{t="students";  e=90},
    @{t="subjects";  e=19},
    @{t="sections";  e=9},
    @{t="rooms";     e=25},
    @{t="teacher_subjects";  e=0},
    @{t="teacher_sections";  e=0},
    @{t="subject_sections";  e=0}
)

Write-Host ""
$header = "   {0,-22} {1,8} {2,10}" -f "Table","Count","Expected"
Write-Host $header -ForegroundColor White
Write-Host ("   " + ("-" * 44)) -ForegroundColor DarkGray

foreach ($c in $counts) {
    $actual = (sql "SELECT COUNT(*) FROM $($c.t);")
    if ($actual) { $actual = $actual.Trim() } else { $actual = "?" }
    $exp = if ($c.e -eq 0) { "varies" } else { "$($c.e)" }
    $color = if ($c.e -eq 0 -or "$actual" -eq "$($c.e)") { "Green" } else { "Yellow" }
    $row = "   {0,-22} {1,8} {2,10}" -f $c.t, $actual, $exp
    Write-Host $row -ForegroundColor $color
}
Write-Host ("   " + ("-" * 44)) -ForegroundColor DarkGray

# Role breakdown
Write-Host ""
logINFO "Role breakdown:"
$roles = sql "SELECT role, COUNT(*) FROM users GROUP BY role ORDER BY role;"
foreach ($line in ($roles -split "`n")) {
    if ($line.Trim()) { logINFO "  $line" }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor White
Write-Host "      SEED DATA INSERTION COMPLETE" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor White
Write-Host ""
Write-Host "  All passwords: password" -ForegroundColor Yellow
Write-Host "  Admin login:   admin / password" -ForegroundColor Yellow
Write-Host "  Emails:        jangamswamy306+{username}@gmail.com" -ForegroundColor Yellow
Write-Host ""
