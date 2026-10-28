-- Master seed data for IPL D11 Stars
INSERT INTO ipld11stars.teams (id, name, short_name, slug, logo_url) VALUES
('CSK', 'Chennai Super Kings', 'CSK', 'chennai-super-kings', 'https://scores.iplt20.com/ipl/teamlogos/CSK.png'),
('MI', 'Mumbai Indians', 'MI', 'mumbai-indians', 'https://scores.iplt20.com/ipl/teamlogos/MI.png'),
('RCB', 'Royal Challengers Bengaluru', 'RCB', 'royal-challengers-bengaluru', 'https://scores.iplt20.com/ipl/teamlogos/rcb.png'),
('KKR', 'Kolkata Knight Riders', 'KKR', 'kolkata-knight-riders', 'https://scores.iplt20.com/ipl/teamlogos/KKR.png'),
('DC', 'Delhi Capitals', 'DC', 'delhi-capitals', 'https://scores.iplt20.com/ipl/teamlogos/DC.png'),
('GT', 'Gujarat Titans', 'GT', 'gujarat-titans', 'https://scores.iplt20.com/ipl/teamlogos/GT.png'),
('RR', 'Rajasthan Royals', 'RR', 'rajasthan-royals', 'https://scores.iplt20.com/ipl/teamlogos/RR.png'),
('PBKS', 'Punjab Kings', 'PBKS', 'punjab-kings', 'https://scores.iplt20.com/ipl/teamlogos/PBKS.png'),
('SRH', 'Sunrisers Hyderabad', 'SRH', 'sunrisers-hyderabad', 'https://scores.iplt20.com/ipl/teamlogos/SRH.png'),
('LSG', 'Lucknow Super Giants', 'LSG', 'lucknow-super-giants', 'https://scores.iplt20.com/ipl/teamlogos/LSG.png')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ipld11stars.seasons (id, name, year, starts_at, ends_at) VALUES
('2026', 'IPL 2026', 2026, '2026-03-20 14:00:00+00', '2026-05-30 18:00:00+00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ipld11stars.players (id, team_id, full_name, role, credits, nationality, batting_style, bowling_style) VALUES
('ply-01', 'RCB', 'Virat Kohli', 'BAT', 10.5, 'Indian', 'Right-hand bat', 'Right-arm medium'),
('ply-02', 'CSK', 'MS Dhoni', 'WK', 9.0, 'Indian', 'Right-hand bat', 'Right-arm medium'),
('ply-03', 'MI', 'Rohit Sharma', 'BAT', 9.5, 'Indian', 'Right-hand bat', 'Right-arm offbreak'),
('ply-04', 'MI', 'Jasprit Bumrah', 'BOWL', 10.0, 'Indian', 'Right-hand bat', 'Right-arm fast'),
('ply-05', 'CSK', 'Ravindra Jadeja', 'AR', 9.5, 'Indian', 'Left-hand bat', 'Slow left-arm orthodox'),
('ply-06', 'MI', 'Hardik Pandya', 'AR', 9.0, 'Indian', 'Right-hand bat', 'Right-arm fast-medium'),
('ply-07', 'DC', 'Rishabh Pant', 'WK', 9.5, 'Indian', 'Left-hand bat', 'Wicketkeeper'),
('ply-08', 'GT', 'Rashid Khan', 'BOWL', 9.5, 'Afghan', 'Right-hand bat', 'Right-arm legbreak'),
('ply-09', 'GT', 'Shubman Gill', 'BAT', 9.5, 'Indian', 'Right-hand bat', 'Right-arm offbreak'),
('ply-10', 'RR', 'Sanju Samson', 'WK', 9.0, 'Indian', 'Right-hand bat', 'Wicketkeeper')
ON CONFLICT (id) DO NOTHING;
